package dev.minekube.craftwright.daemon

import dev.minekube.craftwright.protocol.Client
import dev.minekube.craftwright.protocol.ClientState
import dev.minekube.craftwright.protocol.CreateClientRequest
import dev.minekube.craftwright.protocol.Loader
import dev.minekube.craftwright.protocol.Profile
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalSessionApiServerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `server exposes session metadata and creates fake clients over http`() = withHttpClient { http ->
        LocalSessionApiServer.inMemory().use { server ->
            server.start()

            http.get(server.url("/version")).let { version ->
                assertEquals(HttpStatusCode.OK, version.status)
                assertTrue(version.bodyAsText().contains("\"driver\":\"craftwright-daemon\""))
            }

            http.get(server.url("/openapi.json")).let { openapi ->
                assertEquals(HttpStatusCode.OK, openapi.status)
                assertTrue(openapi.bodyAsText().contains("/player/sendChat"))
            }

            http.get(server.url("/events")).let { events ->
                assertEquals(HttpStatusCode.OK, events.status)
                assertEquals("[]", events.bodyAsText())
            }

            http.post(server.url("/clients")) {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                {
                  "id": "alice",
                  "version": "1.21.4",
                  "loader": "FABRIC",
                  "profile": { "kind": "OFFLINE", "name": "Alice" }
                }
                    """.trimIndent()
                )
            }.let { created ->
                assertEquals(HttpStatusCode.Created, created.status)
                val client = json.decodeFromString<Client>(created.bodyAsText())
                assertEquals("alice", client.id)
                assertEquals(ClientState.RUNNING, client.state)
            }

            http.get(server.url("/clients/alice/events")).let { clientEvents ->
                assertEquals(HttpStatusCode.OK, clientEvents.status)
                assertTrue(clientEvents.bodyAsText().contains("client.created"))
            }
        }
    }

    @Test
    fun `server rejects invalid client creation as bad request`() = withHttpClient { http ->
        LocalSessionApiServer.inMemory().use { server ->
            server.start()

            http.post(server.url("/clients")) {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        CreateClientRequest(
                            id = "bad",
                            version = "1.21.4",
                            loader = Loader.FABRIC,
                            profile = Profile.offline("NameThatIsTooLong"),
                        )
                    )
                )
            }.let { response ->
                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertTrue(response.bodyAsText().contains("offline profile name must be 16 characters or fewer"))
            }
        }
    }

    private fun withHttpClient(block: suspend (HttpClient) -> Unit) {
        kotlinx.coroutines.runBlocking {
            HttpClient(CIO).use { client -> block(client) }
        }
    }
}
