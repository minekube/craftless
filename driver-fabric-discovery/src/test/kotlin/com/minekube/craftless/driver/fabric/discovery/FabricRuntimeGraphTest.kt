package com.minekube.craftless.driver.fabric.discovery

import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeOperationNode
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSourceEvidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FabricRuntimeGraphTest {
    @Test
    fun `runtime graph composes resources and operations from fragments`() {
        val graph =
            fabricRuntimeGraph(
                clientId = "alice",
                fragments =
                    listOf(
                        FabricRuntimeGraphFragment(
                            resources = listOf(RuntimeResourceNode("player", RuntimeAvailability.available())),
                        ),
                        FabricRuntimeGraphFragment(
                            operations =
                                listOf(
                                    RuntimeOperationNode(
                                        id = "player.query",
                                        resource = "player",
                                        adapter = "fabric.player-query",
                                        availability = RuntimeAvailability.available(),
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals("alice", graph.clientId)
        assertEquals(listOf("player"), graph.resources.map { it.id })
        assertEquals(listOf("player.query"), graph.operations.map { it.id })
        assertTrue(graph.fingerprint().startsWith("graph:"))
    }

    @Test
    fun `runtime graph composer preserves graph validation for duplicate nodes`() {
        assertFailsWith<IllegalArgumentException> {
            fabricRuntimeGraph(
                clientId = "alice",
                fragments =
                    listOf(
                        FabricRuntimeGraphFragment(
                            resources = listOf(RuntimeResourceNode("player", RuntimeAvailability.available())),
                        ),
                        FabricRuntimeGraphFragment(
                            resources = listOf(RuntimeResourceNode("player", RuntimeAvailability.available())),
                        ),
                    ),
            )
        }
    }

    @Test
    fun `registry graph fragment exposes available registry resource and handles from metadata evidence`() {
        val metadata =
            DriverRuntimeMetadata
                .runtimeAdapter()
                .copy(registryFingerprint = "registries:abc123")

        val fragment =
            fabricRegistryGraphFragment(
                metadata = metadata,
                available = true,
            )

        assertEquals(listOf("registry"), fragment.resources.map { it.id })
        assertEquals(RuntimeAvailability.available(), fragment.resources.single().availability)
        assertEquals(
            listOf(
                "registry.block",
                "registry.effect",
                "registry.entity",
                "registry.event",
                "registry.item",
                "registry.screen",
            ),
            fragment.handles.map { it.id }.sorted(),
        )
        assertTrue(
            fragment.resources
                .single()
                .sourceEvidence
                .any { evidence -> evidence.kind == "registry" && evidence.fingerprint == "registries:abc123" },
        )
    }

    @Test
    fun `registry graph fragment reports unavailable registry when metadata has no discovery evidence`() {
        val metadata =
            DriverRuntimeMetadata
                .runtimeAdapter()
                .copy(registryFingerprint = "registries:not-discovered")

        val fragment =
            fabricRegistryGraphFragment(
                metadata = metadata,
                available = false,
            )

        val unavailable = RuntimeAvailability.unavailable("registry-not-discovered")
        assertEquals(unavailable, fragment.resources.single().availability)
        assertTrue(fragment.handles.isNotEmpty())
        assertTrue(fragment.handles.all { handle -> handle.availability == unavailable })
    }

    @Test
    fun `event graph fragment exposes available event resource and events from source evidence`() {
        val sourceEvidence = listOf(RuntimeSourceEvidence("event-source", "driver:test"))

        val fragment =
            fabricEventGraphFragment(
                sourceEvidence = sourceEvidence,
                available = true,
            )

        assertEquals(listOf("event"), fragment.resources.map { it.id })
        assertEquals(RuntimeAvailability.available(), fragment.resources.single().availability)
        assertEquals(
            listOf("event.action", "event.capability", "event.lifecycle"),
            fragment.events.map { it.id }.sorted(),
        )
        assertEquals(sourceEvidence, fragment.resources.single().sourceEvidence)
    }

    @Test
    fun `event graph fragment reports unavailable event source with fallback evidence`() {
        val fragment =
            fabricEventGraphFragment(
                sourceEvidence = emptyList(),
                available = false,
            )

        val unavailable = RuntimeAvailability.unavailable("event-source-not-discovered")
        assertEquals(unavailable, fragment.resources.single().availability)
        assertTrue(fragment.events.isNotEmpty())
        assertTrue(fragment.events.all { event -> event.availability == unavailable })
        assertTrue(
            fragment.resources
                .single()
                .sourceEvidence
                .any { evidence -> evidence.kind == "event-source" && evidence.fingerprint == "events:not-discovered" },
        )
    }

    @Test
    fun `client state graph fragment exposes connected resources and handles`() {
        val fragment =
            fabricClientStateGraphFragment(
                FabricClientStateGraphSnapshot(
                    connected = true,
                    player = true,
                    inventory = true,
                    camera = true,
                    interactionManager = true,
                    world = true,
                    recipes = true,
                    recipeCrafting = true,
                ),
            )

        assertEquals(
            listOf("client", "entity", "inventory", "player", "recipe", "screen", "world", "world.block", "world.time"),
            fragment.resources.map { resource -> resource.id }.sorted(),
        )
        assertTrue(fragment.resources.all { resource -> resource.availability == RuntimeAvailability.available() })
        assertEquals(
            listOf("entity.handle", "inventory.slot", "recipe.handle", "world.block.handle"),
            fragment.handles.map { handle -> handle.id }.sorted(),
        )
        assertEquals("world.block", fragment.handles.single { handle -> handle.id == "world.block.handle" }.resource)
        assertTrue(fragment.handles.all { handle -> handle.availability == RuntimeAvailability.available() })
    }

    @Test
    fun `client state graph fragment reports disconnected resources and handles`() {
        val fragment = fabricClientStateGraphFragment(FabricClientStateGraphSnapshot.disconnected())
        val unavailable = RuntimeAvailability.unavailable("client-not-connected")

        assertEquals(RuntimeAvailability.available(), fragment.resources.single { resource -> resource.id == "screen" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "client" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "player" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "inventory" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "recipe" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "world" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "world.block" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "world.time" }.availability)
        assertEquals(unavailable, fragment.resources.single { resource -> resource.id == "entity" }.availability)
        assertTrue(fragment.handles.all { handle -> handle.availability == unavailable })
    }
}
