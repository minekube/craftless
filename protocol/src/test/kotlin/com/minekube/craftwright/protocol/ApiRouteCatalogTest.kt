package com.minekube.craftwright.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiRouteCatalogTest {
    @Test
    fun `catalog exposes required stable session routes`() {
        val catalog = ApiRouteCatalog.sessionDefaults()

        assertEquals("GET", catalog.route("/openapi.json").method)
        assertEquals("GET", catalog.route("/version").method)
        assertEquals("GET", catalog.route("/events").method)
        assertEquals("GET", catalog.route("/client").method)
        assertEquals("GET", catalog.route("/client/state").method)
        assertEquals("GET", catalog.route("GET", "/clients").method)
        assertEquals("POST", catalog.route("POST", "/clients").method)
        assertEquals("GET", catalog.route("GET", "/clients/{id}").method)
        assertEquals("POST", catalog.route("/clients/{id}/connection/connect").method)
        assertEquals("GET", catalog.route("/clients/{id}/openapi.json").method)
        assertEquals("GET", catalog.route("/clients/{id}/actions").method)
        assertEquals("POST", catalog.route("/clients/{id}:run").method)
        assertEquals("POST", catalog.route("/clients/{id}/stop").method)
        assertEquals("GET", catalog.route("/connection").method)
        assertTrue(catalog.routes.none { it.path == "/player" })
        assertTrue(catalog.routes.none { it.path == "/player/position" })
        assertTrue(catalog.routes.none { it.path == "/clients/{id}/player" })
        assertTrue(catalog.routes.none { it.path == "/clients/{id}/player/position" })
        assertTrue(catalog.routes.any { it.path == "/o/{handle}" })
        assertTrue(catalog.routes.any { it.path == "/c/{className}" })
    }
}
