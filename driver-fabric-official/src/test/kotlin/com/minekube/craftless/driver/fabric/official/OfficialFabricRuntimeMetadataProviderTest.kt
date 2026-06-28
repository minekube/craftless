package com.minekube.craftless.driver.fabric.official

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OfficialFabricRuntimeMetadataProviderTest {
    @Test
    fun `snapshot provider emits deterministic official runtime metadata from installed mods`() {
        val snapshot =
            OfficialFabricRuntimeMetadataSnapshot(
                loaderVersion = "0.19.3",
                installedMods =
                    listOf(
                        OfficialFabricInstalledMod("minecraft", "26.2"),
                        OfficialFabricInstalledMod("fabricloader", "0.19.3"),
                        OfficialFabricInstalledMod("fabric-api", "0.153.0+26.2"),
                        OfficialFabricInstalledMod("craftless-driver-fabric-official", "0.1.0-SNAPSHOT"),
                    ),
            )
        val reordered =
            snapshot.copy(installedMods = snapshot.installedMods.reversed())
        val changed =
            snapshot.copy(
                installedMods =
                    snapshot.installedMods.map { mod ->
                        if (mod.id == "fabric-api") {
                            mod.copy(version = "0.154.0+26.2")
                        } else {
                            mod
                        }
                    },
            )

        val metadata = SnapshotOfficialFabricRuntimeMetadataProvider(snapshot).runtimeMetadata("official-probe")
        val repeated = SnapshotOfficialFabricRuntimeMetadataProvider(snapshot).runtimeMetadata("official-probe")
        val reorderedMetadata = SnapshotOfficialFabricRuntimeMetadataProvider(reordered).runtimeMetadata("official-probe")
        val changedMetadata = SnapshotOfficialFabricRuntimeMetadataProvider(changed).runtimeMetadata("official-probe")

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
                    OfficialFabricRuntimeMetadataProvider { clientId ->
                        assertEquals("official-probe", clientId)
                        SnapshotOfficialFabricRuntimeMetadataProvider(
                            OfficialFabricRuntimeMetadataSnapshot(
                                loaderVersion = "0.19.3",
                                installedMods = listOf(OfficialFabricInstalledMod("test-mod", "1.0.0")),
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
}
