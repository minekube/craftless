package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.driver.api.ConnectionTarget
import com.minekube.craftless.driver.api.DriverActionDescriptor
import com.minekube.craftless.driver.api.DriverActionInvocation
import com.minekube.craftless.driver.api.DriverActionResult
import com.minekube.craftless.driver.api.DriverActionStatus
import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import com.minekube.craftless.driver.runtime.DriverBackend
import com.minekube.craftless.driver.runtime.DriverBackendAction
import com.minekube.craftless.driver.runtime.DriverBackendResult
import net.fabricmc.loader.api.FabricLoader
import java.security.MessageDigest

class FabricDriverBackend private constructor(
    private val mode: Mode,
    private val gateway: FabricClientGateway?,
    actionBindings: List<FabricActionBinding> = defaultFabricActionBindings(),
    private val actionDiscovery: FabricActionDiscovery = defaultFabricActionDiscovery(),
    private val runtimeMetadataProvider: FabricRuntimeMetadataProvider = staticFabricRuntimeMetadataProvider(),
) : DriverBackend {
    private val events = mutableListOf<String>()
    private val actionBindingsById = actionBindings.associateBy { it.descriptor.id }

    override fun connect(
        clientId: String,
        target: ConnectionTarget,
    ): DriverBackendResult {
        require(target.host.isNotBlank()) { "connection host is required" }
        require(target.port in 1..65535) { "connection port must be between 1 and 65535" }
        record("connect $clientId ${target.host}:${target.port}")
        gateway?.execute {
            gateway.connect(target)
        }
        return DriverBackendResult(DriverBackendAction.CONNECT, "fabric ${mode.id} connect requested")
    }

    override fun actions(clientId: String): List<DriverActionDescriptor> = discoveredActions(clientId).map { it.descriptor }

    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata = runtimeMetadataProvider.runtimeMetadata(clientId)

    override fun invoke(
        clientId: String,
        invocation: DriverActionInvocation,
    ): DriverActionResult {
        require(invocation.action.isNotBlank()) { "action is required" }
        val discoveredAction = discoveredActions(clientId).firstOrNull { it.descriptor.id == invocation.action }
        if (discoveredAction == null) {
            return DriverActionResult(
                action = invocation.action,
                status = DriverActionStatus.UNSUPPORTED,
                message = "unsupported Fabric action ${invocation.action}",
            )
        }
        val binding = discoveredAction.binding
        if (binding == null) {
            return DriverActionResult(
                action = invocation.action,
                status = DriverActionStatus.UNSUPPORTED,
                message = discoveredAction.descriptor.availabilityReason ?: "unavailable Fabric action ${invocation.action}",
            )
        }
        return binding.invoke(
            clientId = clientId,
            invocation = invocation,
            context =
                FabricActionContext(
                    modeId = mode.id,
                    gateway = gateway,
                    record = ::record,
                ),
        )
    }

    override fun stop(clientId: String): DriverBackendResult {
        record("stop $clientId")
        gateway?.execute {
            gateway.stop()
        }
        return DriverBackendResult(DriverBackendAction.STOP, "fabric ${mode.id} stop requested")
    }

    fun events(): List<String> = events.toList()

    private fun discoveredActions(clientId: String): List<FabricDiscoveredAction> =
        actionDiscovery.discover(
            FabricActionDiscoveryContext(
                clientId = clientId,
                modeId = mode.id,
                gateway = gateway,
                bindings = actionBindingsById,
            ),
        )

    private fun record(event: String) {
        events += event
    }

    private enum class Mode(
        val id: String,
    ) {
        METADATA_ONLY("metadata-only"),
        REAL_CLIENT("real-client"),
    }

    companion object {
        @Volatile
        private var installed: FabricDriverBackend? = null

        fun metadataOnly(): FabricDriverBackend = metadataOnly(defaultFabricActionDiscovery())

        internal fun metadataOnly(actionDiscovery: FabricActionDiscovery): FabricDriverBackend =
            FabricDriverBackend(
                mode = Mode.METADATA_ONLY,
                gateway = null,
                actionDiscovery = actionDiscovery,
            )

        fun real(gateway: FabricClientGateway = MinecraftFabricClientGateway()): FabricDriverBackend =
            real(gateway, defaultFabricActionDiscovery())

        internal fun real(
            gateway: FabricClientGateway,
            actionDiscovery: FabricActionDiscovery,
        ): FabricDriverBackend =
            real(
                gateway = gateway,
                actionDiscovery = actionDiscovery,
                runtimeMetadataProvider = FabricLoaderRuntimeMetadataProvider,
            )

        internal fun real(
            gateway: FabricClientGateway,
            actionDiscovery: FabricActionDiscovery = defaultFabricActionDiscovery(),
            runtimeMetadataProvider: FabricRuntimeMetadataProvider,
        ): FabricDriverBackend =
            FabricDriverBackend(
                mode = Mode.REAL_CLIENT,
                gateway = gateway,
                actionDiscovery = actionDiscovery,
                runtimeMetadataProvider = runtimeMetadataProvider,
            )

        fun install(backend: FabricDriverBackend) {
            installed = backend
        }

        fun current(): FabricDriverBackend = installed ?: metadataOnly().also(::install)
    }
}

internal fun interface FabricRuntimeMetadataProvider {
    fun runtimeMetadata(clientId: String): DriverRuntimeMetadata
}

private fun staticFabricRuntimeMetadataProvider(): FabricRuntimeMetadataProvider =
    FabricRuntimeMetadataProvider {
        DriverRuntimeMetadata(
            loaderVersion = "unknown",
            driver = FABRIC_DRIVER_ID,
            driverVersion = FABRIC_DRIVER_VERSION,
            mappings = FABRIC_MAPPINGS_FINGERPRINT,
            installedModsFingerprint = "mods:metadata-only",
            registryFingerprint = "registries:metadata-only",
            serverFeatureFingerprint = "server-features:metadata-only",
            permissionsFingerprint = "permissions:local-client",
        )
    }

private object FabricLoaderRuntimeMetadataProvider : FabricRuntimeMetadataProvider {
    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata {
        val loader = FabricLoader.getInstance()
        return DriverRuntimeMetadata(
            loaderVersion = loader.versionFor(FABRIC_LOADER_ID) ?: "unknown",
            driver = FABRIC_DRIVER_ID,
            driverVersion = loader.versionFor(FABRIC_DRIVER_ID) ?: FABRIC_DRIVER_VERSION,
            mappings = FABRIC_MAPPINGS_FINGERPRINT,
            installedModsFingerprint = loader.installedModsFingerprint(),
            registryFingerprint = "registries:client-boundary",
            serverFeatureFingerprint = "server-features:${if (loader.isDevelopmentEnvironment) "dev" else "runtime"}",
            permissionsFingerprint = "permissions:local-client",
        )
    }
}

private fun FabricLoader.versionFor(modId: String): String? =
    getModContainer(modId)
        .map { it.metadata.version.friendlyString }
        .orElse(null)

private fun FabricLoader.installedModsFingerprint(): String =
    fingerprint(
        label = "mods",
        values =
            allMods
                .map { "${it.metadata.id}@${it.metadata.version.friendlyString}" }
                .sorted(),
    )

private fun fingerprint(
    label: String,
    values: List<String>,
): String {
    val digest = MessageDigest.getInstance("SHA-256")
    values.forEach { value ->
        digest.update(value.encodeToByteArray())
        digest.update(0)
    }
    return "$label:" + digest.digest().joinToString("") { byte -> "%02x".format(byte) }.take(FINGERPRINT_LENGTH)
}

private const val FABRIC_DRIVER_ID = "craftless-driver-fabric"
private const val FABRIC_DRIVER_VERSION = "0.1.0-SNAPSHOT"
private const val FABRIC_LOADER_ID = "fabricloader"
private const val FABRIC_MAPPINGS_FINGERPRINT = "craftless-fabric-bindings"
private const val FINGERPRINT_LENGTH = 16
