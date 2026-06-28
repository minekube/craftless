package com.minekube.craftless.driver.fabric

import com.minekube.craftless.driver.api.DriverSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

internal class FabricDriverSelfAttach(
    private val http: HttpClient = HttpClient(CIO),
    private val endpointFactory: (DriverSession) -> FabricDriverLoopbackEndpoint = { session ->
        FabricDriverLoopbackEndpoint(session)
    },
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    suspend fun start(
        session: DriverSession,
        environment: FabricDriverAttachEnvironment,
    ): FabricDriverAttachment {
        require(session.clientId == environment.clientId) {
            "driver session client id ${session.clientId} does not match attach client id ${environment.clientId}"
        }
        val endpoint = endpointFactory(session).start()
        var attached = false
        try {
            val response =
                http.post("${environment.daemonUrl}/clients/${environment.clientId}:attach") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(FabricDriverAttachRequest(endpoint.url)))
                }
            check(response.status.value in 200..299) {
                "driver self-attach failed with ${response.status.value}: ${response.bodyAsText()}"
            }
            attached = true
            return FabricDriverAttachment(endpoint)
        } finally {
            if (!attached) {
                endpoint.close()
            }
        }
    }

    companion object {
        private val activeAttachments = CopyOnWriteArrayList<FabricDriverAttachment>()

        fun startFromEnvironment(
            session: DriverSession,
            env: Map<String, String> = System.getenv(),
            selfAttach: FabricDriverSelfAttach = FabricDriverSelfAttach(),
        ) {
            val environment = FabricDriverAttachEnvironment.from(env) ?: return
            startAsync(session = session, environment = environment, selfAttach = selfAttach)
        }

        fun startFromEnvironment(
            sessionFactory: (String) -> DriverSession,
            env: Map<String, String> = System.getenv(),
            selfAttach: FabricDriverSelfAttach = FabricDriverSelfAttach(),
        ) {
            val environment = FabricDriverAttachEnvironment.from(env) ?: return
            startAsync(session = sessionFactory(environment.clientId), environment = environment, selfAttach = selfAttach)
        }

        private fun startAsync(
            session: DriverSession,
            environment: FabricDriverAttachEnvironment,
            selfAttach: FabricDriverSelfAttach,
        ) {
            thread(name = "craftless-driver-self-attach", isDaemon = true) {
                runCatching {
                    runBlocking {
                        selfAttach.start(session = session, environment = environment)
                    }
                }.onSuccess { attachment ->
                    activeAttachments += attachment
                }.onFailure { error ->
                    System.err.println("Craftless driver self-attach failed: ${error.message}")
                }
            }
        }
    }
}

internal class FabricDriverAttachment(
    private val endpoint: FabricDriverLoopbackEndpoint,
) : AutoCloseable {
    val endpointUrl: String = endpoint.url

    override fun close() {
        endpoint.close()
    }
}

@Serializable
private data class FabricDriverAttachRequest(
    val endpoint: String,
)
