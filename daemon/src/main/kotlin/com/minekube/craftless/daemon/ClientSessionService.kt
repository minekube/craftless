package com.minekube.craftless.daemon

import com.minekube.craftless.driver.api.ConnectionTarget
import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import com.minekube.craftless.driver.api.DriverSession
import com.minekube.craftless.protocol.ApiRoute
import com.minekube.craftless.protocol.Client
import com.minekube.craftless.protocol.ClientState
import com.minekube.craftless.protocol.CreateClientRequest
import com.minekube.craftless.protocol.Instance
import com.minekube.craftless.protocol.MAX_OFFLINE_PROFILE_NAME_LENGTH
import com.minekube.craftless.protocol.MinecraftVersion
import com.minekube.craftless.protocol.OpenApiDocument
import com.minekube.craftless.protocol.OpenApiOperation
import com.minekube.craftless.protocol.OpenApiResource
import com.minekube.craftless.protocol.RuntimeCapabilityGraph
import com.minekube.craftless.protocol.isCraftlessClientId

class ClientSessionService private constructor(
    private val driverFactory: DriverSessionFactory,
    private val fileStore: InstanceFileStore?,
) {
    private val clients = linkedMapOf<String, Client>()
    private val drivers = linkedMapOf<String, DriverSession>()
    private val creatingClientIds = mutableSetOf<String>()
    private val stateLock = Any()
    private val failedClients = mutableSetOf<String>()
    private val clientFailureListeners = mutableListOf<(Client) -> Unit>()

    fun createClient(request: CreateClientRequest): Client {
        require(request.id.isCraftlessClientId()) { "client id must be a route-safe segment" }
        require(request.version.isNotBlank()) { "minecraft version is required" }
        val profile = request.resolvedProfile()
        require(profile.name.length <= MAX_OFFLINE_PROFILE_NAME_LENGTH) { "offline profile name must be 16 characters or fewer" }
        synchronized(stateLock) {
            require(!clients.containsKey(request.id) && creatingClientIds.add(request.id)) { "client ${request.id} already exists" }
        }

        var registered = false
        try {
            val instance =
                Instance(
                    id = "${request.id}-${request.version}-${request.loader.name.lowercase()}",
                    version = MinecraftVersion(request.version),
                    loader = request.loader,
                )
            fileStore?.prepare(instance.files)
            val driver = driverFactory.create(request)
            val initialState = driver.snapshot().state
            val client =
                Client(
                    id = request.id,
                    instance = instance,
                    profile = profile,
                    presentation = request.presentation,
                    state = ClientState.CREATED,
                )
            return synchronized(stateLock) {
                require(!clients.containsKey(request.id)) { "client ${request.id} already exists" }
                clients[request.id] = client
                drivers[request.id] = driver
                creatingClientIds.remove(request.id)
                registered = true
                updateStateLocked(request.id, initialState)
            }
        } finally {
            if (!registered) {
                synchronized(stateLock) {
                    creatingClientIds.remove(request.id)
                }
            }
        }
    }

    fun listClients(): List<Client> {
        val clientIds = synchronized(stateLock) { clients.keys.toList() }
        return clientIds.map(::client)
    }

    fun client(clientId: String): Client {
        val (client, driver) = synchronized(stateLock) {
            val current = clients[clientId] ?: error("client $clientId not found")
            current to drivers[clientId]
        }
        val liveness = driver as? ClientRuntimeLiveness ?: return synchronized(stateLock) {
            clients[clientId] ?: error("client $clientId not found")
        }
        val liveState = liveness.liveState()
        return synchronized(stateLock) {
            val current = clients[clientId] ?: error("client $clientId not found")
            if (drivers[clientId] !== driver) {
                return@synchronized current
            }
            if (liveState == client.state) current else updateStateLocked(clientId, liveState)
        }
    }

    /** The reason a client runtime died on its own, when the daemon owns its process. */
    fun runtimeFailure(clientId: String): String? {
        val driver = synchronized(stateLock) { drivers[clientId] }
        return (driver as? ClientRuntimeLiveness)?.failureMessage()
    }

    fun observeAllClients() {
        val clientIds = synchronized(stateLock) { clients.keys.toList() }
        clientIds.forEach(::client)
    }

    fun onClientFailed(listener: (Client) -> Unit) {
        synchronized(stateLock) {
            clientFailureListeners += listener
        }
    }

    fun driverFor(clientId: String): DriverSession = synchronized(stateLock) {
        drivers[clientId] ?: error("client $clientId not found")
    }

    fun attachDriver(
        clientId: String,
        driver: DriverSession,
    ): Client {
        require(driver.clientId == clientId) { "attached driver client id must match $clientId" }
        synchronized(stateLock) {
            require(clients.containsKey(clientId)) { "client $clientId not found" }
        }
        val state = driver.snapshot().state
        return synchronized(stateLock) {
            val current = observeCurrentRuntimeLocked(clientId)
            if (current.state == ClientState.FAILED) {
                current
            } else {
                drivers[clientId] = driver
                updateStateLocked(clientId, state)
            }
        }
    }

    fun connectClient(
        clientId: String,
        target: ConnectionTarget,
    ): Client {
        val (initialClient, currentDriver) = synchronized(stateLock) {
            val current = observeCurrentRuntimeLocked(clientId)
            current to drivers[clientId]
        }
        if (initialClient.state == ClientState.FAILED) return initialClient
        val driver = currentDriver ?: error("client $clientId not found")
        val snapshot = driver.connect(target)
        return synchronized(stateLock) {
            val current = observeCurrentRuntimeLocked(clientId)
            if (current.state == ClientState.FAILED) {
                current
            } else if (drivers[clientId] !== currentDriver) {
                current
            } else {
                updateStateLocked(clientId, snapshot.state)
            }
        }
    }

    fun stopClient(clientId: String): Client {
        val driver = driverFor(clientId)
        val snapshot = driver.stop()
        return synchronized(stateLock) {
            if (drivers[clientId] !== driver) {
                clients[clientId] ?: error("client $clientId not found")
            } else {
                updateStateLocked(clientId, snapshot.state)
            }
        }
    }

    fun routesFor(clientId: String): List<ApiRoute> {
        synchronized(stateLock) {
            require(clients.containsKey(clientId)) { "client $clientId not found" }
        }
        return openApiFor(clientId).toApiRoutes(clientId)
    }

    fun resourcesFor(clientId: String): List<OpenApiResource> = openApiFor(clientId).resources

    fun openApiFor(clientId: String): OpenApiDocument {
        val client = client(clientId)
        val driver = driverFor(clientId)
        val graph = driver.runtimeGraph()
        val metadata =
            RuntimeOpenApiMetadata.forGraph(
                client = client,
                graph = graph,
                metadata = driver.runtimeMetadata(),
            )
        return OpenApiDocument
            .fromRuntimeGraph(
                graph = graph,
                extensions = metadata.extensions,
            ).withConcreteClientId(clientId)
    }

    companion object {
        fun inMemory(driverFactory: DriverSessionFactory): ClientSessionService = ClientSessionService(driverFactory, fileStore = null)

        fun inMemory(
            driverFactory: DriverSessionFactory = DriverSessionFactory.unavailable(),
            fileStore: InstanceFileStore? = null,
        ): ClientSessionService = ClientSessionService(driverFactory, fileStore)
    }

    private fun updateStateLocked(
        clientId: String,
        state: ClientState,
    ): Client {
        val current = clients[clientId] ?: error("client $clientId not found")
        if (current.state == state) return current
        val updated = current.copy(state = state)
        clients[clientId] = updated
        if (state == ClientState.FAILED && failedClients.add(clientId)) {
            clientFailureListeners.forEach { listener -> listener(updated) }
        }
        return updated
    }

    private fun observeCurrentRuntimeLocked(clientId: String): Client {
        val current = clients[clientId] ?: error("client $clientId not found")
        if (current.state == ClientState.FAILED) return current
        val liveness = drivers[clientId] as? ClientRuntimeLiveness ?: return current
        val liveState = liveness.liveState()
        return if (liveState == current.state) current else updateStateLocked(clientId, liveState)
    }
}

private fun OpenApiDocument.withConcreteClientId(clientId: String): OpenApiDocument =
    copy(
        paths =
            paths.mapKeys { (path, _) ->
                path.replace("/clients/{id}", "/clients/$clientId")
            },
    )

private fun OpenApiDocument.toApiRoutes(clientId: String): List<ApiRoute> {
    val prefix = "/clients/$clientId"
    return paths
        .filterKeys { path -> path == prefix || path.startsWith("$prefix/") || path.startsWith("$prefix:") }
        .flatMap { (path, operations) ->
            listOfNotNull(
                operations.get?.toApiRoute(method = "GET", path = path),
                operations.post?.toApiRoute(method = "POST", path = path),
            )
        }
}

private fun OpenApiOperation.toApiRoute(
    method: String,
    path: String,
): ApiRoute {
    val owner = requireNotNull(extensions["x-craftless-owner"]) { "openapi operation $operationId is missing owner metadata" }
    val target = requireNotNull(extensions["x-craftless-target"]) { "openapi operation $operationId is missing target metadata" }
    val source = requireNotNull(extensions["x-craftless-source"]) { "openapi operation $operationId is missing source metadata" }
    val returnKind = requireNotNull(extensions["x-craftless-return"]) { "openapi operation $operationId is missing return metadata" }
    return ApiRoute(
        method = method,
        path = path,
        operationId = operationId,
        tag = tags.firstOrNull() ?: owner,
        owner = owner,
        member = extensions["x-craftless-member"],
        target = target,
        source = source,
        returnKind = returnKind,
        actionId = extensions["x-craftless-action"],
    )
}

fun interface DriverSessionFactory {
    fun create(request: CreateClientRequest): DriverSession

    companion object {
        fun unavailable(): DriverSessionFactory =
            DriverSessionFactory { request ->
                error("no Craftless driver runtime configured for client ${request.id}")
            }
    }
}

private data class RuntimeOpenApiMetadata(
    val extensions: Map<String, String>,
) {
    companion object {
        fun forGraph(
            client: Client,
            graph: RuntimeCapabilityGraph,
            metadata: DriverRuntimeMetadata,
        ): RuntimeOpenApiMetadata {
            val graphFingerprint = graph.fingerprint()
            val extensions =
                linkedMapOf(
                    "x-craftless-client-id" to client.id,
                    "x-craftless-minecraft-version" to client.instance.version.id,
                    "x-craftless-loader" to client.instance.loader.name,
                    "x-craftless-loader-version" to metadata.loaderVersion,
                    "x-craftless-driver" to metadata.driver,
                    "x-craftless-driver-version" to metadata.driverVersion,
                    "x-craftless-mappings-fingerprint" to metadata.mappings,
                    "x-craftless-installed-mods-fingerprint" to metadata.installedModsFingerprint,
                    "x-craftless-registry-fingerprint" to metadata.registryFingerprint,
                    "x-craftless-server-feature-fingerprint" to metadata.serverFeatureFingerprint,
                    "x-craftless-permissions-fingerprint" to metadata.permissionsFingerprint,
                    "x-craftless-action-schema-versions" to graphFingerprint,
                    "x-craftless-action-fingerprint" to graphFingerprint,
                    "runtimeGraphFingerprint" to graphFingerprint,
                    "x-craftless-runtime-fingerprint" to graphFingerprint,
                )
            return RuntimeOpenApiMetadata(extensions)
        }
    }
}
