package com.minekube.craftless.driver.fabric.attach

import com.minekube.craftless.driver.api.DriverSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class FabricDriverSelfAttach(
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
            for (attempt in 1..SELF_ATTACH_MAX_ATTEMPTS) {
                val response =
                    http.post("${environment.daemonUrl}/clients/${environment.clientId}:attach") {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(FabricDriverAttachRequest(endpoint.url)))
                    }
                if (response.status.value in 200..299) {
                    attached = true
                    return FabricDriverAttachment(endpoint)
                }

                val body = response.bodyAsText()
                val registrationRace =
                    response.status == HttpStatusCode.NotFound &&
                        runCatching {
                            json.decodeFromString<FabricDriverAttachErrorResponse>(body).code == MISSING_CLIENT_CODE
                        }.getOrDefault(false)
                check(registrationRace && attempt < SELF_ATTACH_MAX_ATTEMPTS) {
                    "driver self-attach failed with ${response.status.value}: $body"
                }
                delay(SELF_ATTACH_RETRY_DELAY_MILLIS)
            }
            error("driver self-attach exhausted its bounded registration retry budget")
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

class FabricDriverAttachment(
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

@Serializable
private data class FabricDriverAttachErrorResponse(
    val code: String,
)

private const val MISSING_CLIENT_CODE = "MISSING_CLIENT"
private const val SELF_ATTACH_MAX_ATTEMPTS = 50
private const val SELF_ATTACH_RETRY_DELAY_MILLIS = 100L
