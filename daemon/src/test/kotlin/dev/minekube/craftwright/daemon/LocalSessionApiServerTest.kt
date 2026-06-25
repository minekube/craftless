package dev.minekube.craftwright.daemon

import dev.minekube.craftwright.protocol.Client
import dev.minekube.craftwright.protocol.ClientState
import dev.minekube.craftwright.protocol.CreateClientRequest
import dev.minekube.craftwright.protocol.Loader
import dev.minekube.craftwright.protocol.Profile
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalSessionApiServerTest {
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json".toMediaType()

    @Test
    fun `server exposes session metadata and creates fake clients over http`() {
        LocalSessionApiServer.inMemory().use { server ->
            server.start()

            get(server, "/version").use { version ->
                assertEquals(200, version.code)
                assertTrue(version.body.string().contains("\"driver\":\"craftwright-daemon\""))
            }

            get(server, "/openapi.json").use { openapi ->
                assertEquals(200, openapi.code)
                assertTrue(openapi.body.string().contains("/player/sendChat"))
            }

            get(server, "/events").use { events ->
                assertEquals(200, events.code)
                assertEquals("[]", events.body.string())
            }

            postJson(
                server,
                "/clients",
                """
                {
                  "id": "alice",
                  "version": "1.21.4",
                  "loader": "FABRIC",
                  "profile": { "kind": "OFFLINE", "name": "Alice" }
                }
                """.trimIndent(),
            ).use { created ->
                assertEquals(201, created.code)
                val client = json.decodeFromString<Client>(created.body.string())
                assertEquals("alice", client.id)
                assertEquals(ClientState.RUNNING, client.state)
            }

            get(server, "/clients/alice/events").use { clientEvents ->
                assertEquals(200, clientEvents.code)
                assertTrue(clientEvents.body.string().contains("client.created"))
            }
        }
    }

    @Test
    fun `server rejects invalid client creation as bad request`() {
        LocalSessionApiServer.inMemory().use { server ->
            server.start()

            postJson(
                server,
                "/clients",
                json.encodeToString(
                    CreateClientRequest(
                        id = "bad",
                        version = "1.21.4",
                        loader = Loader.FABRIC,
                        profile = Profile.offline("NameThatIsTooLong"),
                    )
                ),
            ).use { response ->
                assertEquals(400, response.code)
                assertTrue(response.body.string().contains("offline profile name must be 16 characters or fewer"))
            }
        }
    }

    private fun get(server: LocalSessionApiServer, path: String) =
        http.newCall(Request.Builder().url(server.url(path)).build()).execute()

    private fun postJson(server: LocalSessionApiServer, path: String, body: String) =
        http.newCall(
            Request.Builder()
                .url(server.url(path))
                .post(body.toRequestBody(jsonMediaType))
                .build()
        ).execute()
}
