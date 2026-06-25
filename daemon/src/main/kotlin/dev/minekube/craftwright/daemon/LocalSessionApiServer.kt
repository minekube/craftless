package dev.minekube.craftwright.daemon

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.minekube.craftwright.protocol.ApiRouteCatalog
import dev.minekube.craftwright.protocol.Client
import dev.minekube.craftwright.protocol.CreateClientRequest
import dev.minekube.craftwright.protocol.OpenApiDocument
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.Executors

class LocalSessionApiServer private constructor(
    private val service: ClientSessionService,
    bindAddress: InetAddress,
    port: Int,
) : AutoCloseable {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val events = mutableListOf<SessionEvent>()
    private val server = HttpServer.create(InetSocketAddress(bindAddress, port), 0)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "craftwright-local-api").apply { isDaemon = true }
    }

    init {
        server.executor = executor
        server.createContext("/") { exchange -> handle(exchange) }
    }

    fun start() {
        server.start()
    }

    fun url(path: String): String =
        "http://127.0.0.1:${server.address.port}$path"

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun handle(exchange: HttpExchange) {
        try {
            when {
                exchange.requestMethod == "GET" && exchange.requestURI.path == "/version" ->
                    exchange.writeJson(200, RuntimeVersion.current())

                exchange.requestMethod == "GET" && exchange.requestURI.path == "/openapi.json" ->
                    exchange.writeJson(200, OpenApiDocument.from(ApiRouteCatalog.sessionDefaults()))

                exchange.requestMethod == "GET" && exchange.requestURI.path == "/events" ->
                    exchange.writeJson(200, events)

                exchange.requestMethod == "POST" && exchange.requestURI.path == "/clients" ->
                    createClient(exchange)

                exchange.requestMethod == "GET" && exchange.requestURI.path.matches(Regex("""/clients/[^/]+/events""")) ->
                    clientEvents(exchange)

                else ->
                    exchange.writeJson(404, ErrorResponse("NOT_FOUND", "route not found: ${exchange.requestURI.path}"))
            }
        } catch (error: IllegalArgumentException) {
            exchange.writeJson(400, ErrorResponse("BAD_REQUEST", error.message ?: "bad request"))
        } catch (error: Exception) {
            exchange.writeJson(500, ErrorResponse("INTERNAL_ERROR", error.message ?: "internal error"))
        }
    }

    private fun createClient(exchange: HttpExchange) {
        val request = json.decodeFromString<CreateClientRequest>(exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8))
        val client = service.createClient(request)
        events += SessionEvent(
            type = "client.created",
            client = client.id,
            message = "created client ${client.id}",
        )
        exchange.writeJson(201, client)
    }

    private fun clientEvents(exchange: HttpExchange) {
        val clientId = exchange.requestURI.path.removePrefix("/clients/").removeSuffix("/events")
        service.routesFor(clientId)
        exchange.writeJson(200, events.filter { it.client == clientId })
    }

    private inline fun <reified T> HttpExchange.writeJson(status: Int, value: T) {
        val bytes = json.encodeToString(value).toByteArray(StandardCharsets.UTF_8)
        responseHeaders.set("Content-Type", "application/json")
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    companion object {
        fun inMemory(port: Int = 0): LocalSessionApiServer =
            LocalSessionApiServer(
                service = ClientSessionService.inMemory(),
                bindAddress = InetAddress.getLoopbackAddress(),
                port = port,
            )
    }
}

@Serializable
data class RuntimeVersion(
    val minecraft: String,
    val loader: String,
    val loaderVersion: String,
    val driver: String,
    val driverVersion: String,
    val java: String,
    val mappings: String,
    val openapiGeneratedAt: String,
) {
    companion object {
        fun current(now: Instant = Instant.now()): RuntimeVersion =
            RuntimeVersion(
                minecraft = "fake",
                loader = "none",
                loaderVersion = "none",
                driver = "craftwright-daemon",
                driverVersion = "0.1.0-SNAPSHOT",
                java = Runtime.version().feature().toString(),
                mappings = "none",
                openapiGeneratedAt = now.toString(),
            )
    }
}

@Serializable
data class SessionEvent(
    val type: String,
    val client: String? = null,
    val message: String? = null,
    val time: String = Instant.now().toString(),
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)
