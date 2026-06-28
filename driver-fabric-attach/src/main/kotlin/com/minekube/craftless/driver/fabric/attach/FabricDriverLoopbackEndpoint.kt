package com.minekube.craftless.driver.fabric.attach

import com.minekube.craftless.driver.api.ConnectionTarget
import com.minekube.craftless.driver.api.DriverActionInvocation
import com.minekube.craftless.driver.api.DriverSession
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.ServerSocket

class FabricDriverLoopbackEndpoint(
    private val session: DriverSession,
    private val host: String = LOOPBACK_HOST,
    requestedPort: Int = 0,
) : AutoCloseable {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private val port = if (requestedPort == 0) allocateLoopbackPort() else requestedPort
    private val engine =
        embeddedServer(CIO, host = host, port = port) {
            installRoutes()
        }

    val url: String = "http://$host:$port"

    fun start(): FabricDriverLoopbackEndpoint {
        engine.start(wait = false)
        return this
    }

    override fun close() {
        engine.stop(gracePeriodMillis = 250, timeoutMillis = 1_000)
    }

    private fun Application.installRoutes() {
        routing {
            get("/snapshot") {
                call.respondJson(session.snapshot())
            }
            post("/connect") {
                val target = json.decodeFromString<ConnectionTarget>(call.receiveText())
                call.respondJson(session.connect(target))
            }
            get("/actions") {
                call.respondJson(session.actions())
            }
            get("/runtime-metadata") {
                call.respondJson(session.runtimeMetadata())
            }
            get("/runtime-graph") {
                call.respondJson(session.runtimeGraph())
            }
            post("/invoke") {
                val invocation = json.decodeFromString<DriverActionInvocation>(call.receiveText())
                call.respondJson(session.invoke(invocation))
            }
            post("/stop") {
                call.receiveText()
                call.respondJson(session.stop())
            }
            get("/events") {
                call.respondJson(session.events())
            }
        }
    }

    private suspend inline fun <reified T> ApplicationCall.respondJson(value: T) {
        respondText(json.encodeToString(value), ContentType.Application.Json)
    }

    private fun allocateLoopbackPort(): Int =
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            socket.localPort
        }

    private companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
    }
}
