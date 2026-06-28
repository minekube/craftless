package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.driver.api.ConnectionTarget
import com.minekube.craftless.driver.fabric.discovery.FabricClientStateGraphSnapshot
import com.minekube.craftless.driver.fabric.discovery.FabricRuntimeMetadataProvider
import com.minekube.craftless.driver.fabric.discovery.FabricRuntimeMetadataSnapshot
import com.minekube.craftless.driver.fabric.discovery.SnapshotFabricRuntimeMetadataProvider
import com.minekube.craftless.driver.fabric.discovery.fabricRuntimeFingerprint
import com.minekube.craftless.driver.runtime.DriverBackendAction
import com.minekube.craftless.protocol.RuntimeAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OfficialFabricSharedRuntimeMetadataTest {
    @Test
    fun `snapshot provider emits deterministic official runtime metadata from installed mods`() {
        val snapshot =
            FabricRuntimeMetadataSnapshot(
                loaderVersion = "0.19.3",
                driver = "craftless-driver-fabric-official",
                driverVersion = "0.1.0-SNAPSHOT",
                mappings = "craftless-official-bindings-26-2",
                installedModsFingerprint =
                    fabricRuntimeFingerprint(
                        "mods",
                        listOf(
                            "minecraft@26.2",
                            "fabricloader@0.19.3",
                            "fabric-api@0.153.0+26.2",
                            "craftless-driver-fabric-official@0.1.0-SNAPSHOT",
                        ),
                    ),
                registryFingerprint = "registries:not-discovered",
                serverFeatureFingerprint = "server-features:not-connected",
                permissionsFingerprint = "permissions:local-client",
            )
        val reordered =
            snapshot.copy(
                installedModsFingerprint =
                    fabricRuntimeFingerprint(
                        "mods",
                        listOf(
                            "craftless-driver-fabric-official@0.1.0-SNAPSHOT",
                            "fabric-api@0.153.0+26.2",
                            "fabricloader@0.19.3",
                            "minecraft@26.2",
                        ),
                    ),
            )
        val changed =
            snapshot.copy(
                installedModsFingerprint =
                    fabricRuntimeFingerprint(
                        "mods",
                        listOf(
                            "minecraft@26.2",
                            "fabricloader@0.19.3",
                            "fabric-api@0.154.0+26.2",
                            "craftless-driver-fabric-official@0.1.0-SNAPSHOT",
                        ),
                    ),
            )

        val metadata = SnapshotFabricRuntimeMetadataProvider(snapshot).runtimeMetadata("official-probe")
        val repeated = SnapshotFabricRuntimeMetadataProvider(snapshot).runtimeMetadata("official-probe")
        val reorderedMetadata = SnapshotFabricRuntimeMetadataProvider(reordered).runtimeMetadata("official-probe")
        val changedMetadata = SnapshotFabricRuntimeMetadataProvider(changed).runtimeMetadata("official-probe")

        assertEquals("0.19.3", metadata.loaderVersion)
        assertEquals("craftless-driver-fabric-official", metadata.driver)
        assertEquals("0.1.0-SNAPSHOT", metadata.driverVersion)
        assertEquals("craftless-official-bindings-26-2", metadata.mappings)
        assertTrue(metadata.installedModsFingerprint.startsWith("mods:"))
        assertEquals(metadata.installedModsFingerprint, repeated.installedModsFingerprint)
        assertEquals(metadata.installedModsFingerprint, reorderedMetadata.installedModsFingerprint)
        assertNotEquals(metadata.installedModsFingerprint, changedMetadata.installedModsFingerprint)
        assertEquals("registries:not-discovered", metadata.registryFingerprint)
        assertEquals("server-features:not-connected", metadata.serverFeatureFingerprint)
        assertEquals("permissions:local-client", metadata.permissionsFingerprint)
    }

    @Test
    fun `official backend delegates runtime metadata to provider`() {
        val backend =
            OfficialFabricDriverBackend(
                runtimeMetadataProvider =
                    FabricRuntimeMetadataProvider { clientId ->
                        assertEquals("official-probe", clientId)
                        SnapshotFabricRuntimeMetadataProvider(
                            FabricRuntimeMetadataSnapshot(
                                loaderVersion = "0.19.3",
                                driver = "craftless-driver-fabric-official",
                                driverVersion = "0.1.0-SNAPSHOT",
                                mappings = "craftless-official-bindings-26-2",
                                installedModsFingerprint = fabricRuntimeFingerprint("mods", listOf("test-mod@1.0.0")),
                                registryFingerprint = "registries:not-discovered",
                                serverFeatureFingerprint = "server-features:not-connected",
                                permissionsFingerprint = "permissions:local-client",
                            ),
                        ).runtimeMetadata(clientId)
                    },
            )

        val metadata = backend.runtimeMetadata("official-probe")

        assertEquals("0.19.3", metadata.loaderVersion)
        assertEquals("craftless-driver-fabric-official", metadata.driver)
        assertTrue(metadata.installedModsFingerprint.startsWith("mods:"))
        assertNotEquals("mods:official-lane-probe", metadata.installedModsFingerprint)
    }

    @Test
    fun `official runtime metadata uses lane server feature provider`() {
        val provider =
            officialFabricRuntimeMetadataProvider(
                serverFeatureProvider =
                    OfficialFabricServerFeatureProvider {
                        listOf("connection:connected", "server:remote", "feature-set:abc123")
                    },
            )

        val metadata = provider.runtimeMetadata("official-probe")

        assertTrue(metadata.serverFeatureFingerprint.startsWith("server-features:"))
        assertNotEquals("server-features:not-connected", metadata.serverFeatureFingerprint)
    }

    @Test
    fun `official backend connect delegates to lifecycle connector`() {
        val target = ConnectionTarget(host = "127.0.0.1", port = 25565)
        val observedTargets = mutableListOf<ConnectionTarget>()
        val backend =
            OfficialFabricDriverBackend(
                runtimeMetadataProvider =
                    FabricRuntimeMetadataProvider { clientId ->
                        SnapshotFabricRuntimeMetadataProvider(
                            FabricRuntimeMetadataSnapshot(
                                loaderVersion = "0.19.3",
                                driver = "craftless-driver-fabric-official",
                                driverVersion = "0.1.0-SNAPSHOT",
                                mappings = "craftless-official-bindings-26-2",
                                installedModsFingerprint = fabricRuntimeFingerprint("mods", listOf("test-mod@1.0.0")),
                                registryFingerprint = "registries:not-discovered",
                                serverFeatureFingerprint = "server-features:not-connected",
                                permissionsFingerprint = "permissions:local-client",
                            ),
                        ).runtimeMetadata(clientId)
                    },
                clientStateProvider = OfficialFabricClientStateProvider { FabricClientStateGraphSnapshot.disconnected() },
                clientConnector =
                    object : OfficialFabricClientConnector {
                        override fun connect(target: ConnectionTarget): Boolean {
                            observedTargets += target
                            return true
                        }
                    },
            )

        val result = backend.connect("official-test", target)

        assertEquals(listOf(target), observedTargets)
        assertEquals(DriverBackendAction.CONNECT, result.action)
        assertTrue(result.observed)
        val message = result.message.orEmpty()
        assertTrue(message.contains("official-test"))
        assertTrue(message.contains("127.0.0.1:25565"))
    }

    @Test
    fun `official lane has production minecraft client connector`() {
        assertEquals("MinecraftOfficialFabricClientConnector", MinecraftOfficialFabricClientConnector::class.simpleName)
    }

    @Test
    fun `official backend projects client state from lane provider without adding operations`() {
        val runtimeMetadataProvider =
            officialFabricRuntimeMetadataProvider(
                serverFeatureProvider =
                    OfficialFabricServerFeatureProvider {
                        listOf("connection:connected", "server:remote", "feature-set:abc123")
                    },
            )
        val backend =
            OfficialFabricDriverBackend(
                runtimeMetadataProvider = runtimeMetadataProvider,
                clientStateProvider =
                    OfficialFabricClientStateProvider {
                        FabricClientStateGraphSnapshot(
                            connected = true,
                            player = true,
                            inventory = true,
                            camera = true,
                            interactionManager = true,
                            world = true,
                            recipes = false,
                            recipeCrafting = false,
                        )
                    },
            )

        val metadata = runtimeMetadataProvider.runtimeMetadata("official-probe")
        val graph = backend.runtimeGraph("official-probe")
        val resources = graph.resources.associateBy { resource -> resource.id }
        val handles = graph.handles.associateBy { handle -> handle.id }
        val runtimeEvidence =
            resources
                .getValue("runtime")
                .sourceEvidence
                .associateBy { evidence -> evidence.kind }

        assertEquals(emptyList(), graph.operations)
        assertTrue(metadata.serverFeatureFingerprint.startsWith("server-features:"))
        assertNotEquals("server-features:not-connected", metadata.serverFeatureFingerprint)
        assertEquals(metadata.serverFeatureFingerprint, runtimeEvidence.getValue("server-features").fingerprint)
        assertEquals(RuntimeAvailability.available(), resources.getValue("client").availability)
        assertEquals(RuntimeAvailability.available(), resources.getValue("player").availability)
        assertEquals(RuntimeAvailability.available(), resources.getValue("inventory").availability)
        assertEquals(RuntimeAvailability.available(), resources.getValue("world").availability)
        assertEquals(RuntimeAvailability.available(), resources.getValue("entity").availability)
        assertEquals(RuntimeAvailability.unavailable("recipe-discovery-unavailable"), resources.getValue("recipe").availability)
        assertEquals(RuntimeAvailability.available(), handles.getValue("inventory.slot").availability)
        assertEquals(RuntimeAvailability.available(), handles.getValue("world.block.handle").availability)
        assertEquals(RuntimeAvailability.available(), handles.getValue("entity.handle").availability)
        assertEquals(RuntimeAvailability.unavailable("recipe-discovery-unavailable"), handles.getValue("recipe.handle").availability)
    }
}
