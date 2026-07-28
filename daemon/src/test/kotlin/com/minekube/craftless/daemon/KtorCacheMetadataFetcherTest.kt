package com.minekube.craftless.daemon

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KtorCacheMetadataFetcherTest {
    @Test
    fun `transient upstream server errors are retried`() {
        val attempts = AtomicInteger()

        withUpstream({
            routing {
                get("/flaky") {
                    if (attempts.incrementAndGet() < 3) {
                        call.respondText("upstream hiccup", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
                    } else {
                        call.respondText("cache metadata payload", ContentType.Text.Plain)
                    }
                }
            }
        }) { baseUrl ->
            val body = runBlocking { KtorCacheMetadataFetcher().fetchText("$baseUrl/flaky") }

            assertEquals("cache metadata payload", body)
            assertEquals(3, attempts.get())
        }
    }

    @Test
    fun `a missing artifact is not retried`() {
        // A permanent 4xx is a real answer about the version, not a hiccup. Retrying
        // it would delay a genuine signal without ever fixing it.
        val attempts = AtomicInteger()

        withUpstream({
            routing {
                get("/missing") {
                    attempts.incrementAndGet()
                    call.respondText("nope", ContentType.Text.Plain, HttpStatusCode.NotFound)
                }
            }
        }) { baseUrl ->
            val failure =
                assertFailsWith<IllegalArgumentException> {
                    runBlocking { KtorCacheMetadataFetcher().fetchText("$baseUrl/missing") }
                }

            assertTrue(failure.message?.contains("404") == true, failure.message)
            assertEquals(1, attempts.get())
        }
    }

    @Test
    fun `binary artifact fetches retry transient faults too`() {
        val attempts = AtomicInteger()

        withUpstream({
            routing {
                get("/artifact") {
                    if (attempts.incrementAndGet() < 2) {
                        call.respondText("gateway", ContentType.Text.Plain, HttpStatusCode.BadGateway)
                    } else {
                        call.respondText("artifact bytes", ContentType.Text.Plain)
                    }
                }
            }
        }) { baseUrl ->
            val bytes = runBlocking { KtorCacheMetadataFetcher().fetchBytes("$baseUrl/artifact") }

            assertEquals("artifact bytes", bytes.decodeToString())
            assertEquals(2, attempts.get())
        }
    }
}

private fun withUpstream(
    configure: Application.() -> Unit,
    block: (String) -> Unit,
) {
    val port = ServerSocket(0).use { socket -> socket.localPort }
    val engine = embeddedServer(CIO, host = "127.0.0.1", port = port, module = configure)
    engine.start(wait = false)
    try {
        block("http://127.0.0.1:$port")
    } finally {
        engine.stop(gracePeriodMillis = 0, timeoutMillis = 2_000)
    }
}
