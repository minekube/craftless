package com.minekube.craftless.driver.fabric

import com.minekube.craftless.driver.fabric.runtime.FabricCompatibilityStatus
import com.minekube.craftless.driver.fabric.runtime.FabricCompiledLaneMetadata
import com.minekube.craftless.driver.fabric.runtime.FabricRuntimeIdentity
import com.minekube.craftless.driver.fabric.runtime.defaultFabricCompatibilityMatrix
import kotlin.test.Test
import kotlin.test.assertEquals

class FabricBootstrapSelectorTest {
    @Test
    fun `selector exposes current compiled lane bootstrap metadata without initialization`() {
        val bootstrap = FabricBootstrapSelector.selectCurrentCompiledLane()

        assertEquals(FabricCompiledLaneMetadata.PROVIDER_ID, bootstrap.providerId)
        assertEquals(FabricCompiledLaneMetadata.MINECRAFT_VERSION, bootstrap.minecraftVersion)
    }

    @Test
    fun `selector exposes registered bootstrap metadata without initialization`() {
        val bootstraps = FabricBootstrapSelector.registeredBootstraps()

        assertEquals(
            listOf(FabricCompiledLaneMetadata.PROVIDER_ID to FabricCompiledLaneMetadata.MINECRAFT_VERSION),
            bootstraps.map { bootstrap -> bootstrap.providerId to bootstrap.minecraftVersion },
        )
    }

    @Test
    fun `selector current bootstrap matches supported compatibility lane`() {
        val bootstrap = FabricBootstrapSelector.selectCurrentCompiledLane()
        val lane = defaultFabricCompatibilityMatrix().resolve(currentLaneIdentity())

        assertEquals(FabricCompatibilityStatus.SUPPORTED, lane.status)
        assertEquals(lane.providerId, bootstrap.providerId)
        assertEquals(lane.gameVersion, bootstrap.minecraftVersion)
    }

    private fun currentLaneIdentity(): FabricRuntimeIdentity =
        FabricRuntimeIdentity(
            gameVersion = FabricCompiledLaneMetadata.MINECRAFT_VERSION,
            loaderVersion = FabricCompiledLaneMetadata.LOADER_VERSION,
            fabricApiVersion = FabricCompiledLaneMetadata.FABRIC_API_VERSION,
            mappingsFingerprint = FabricCompiledLaneMetadata.MAPPINGS_FINGERPRINT,
            installedModsFingerprint = "mods:current-lane",
            registryFingerprint = "registries:current-lane",
            serverFeatureFingerprint = "server-features:local",
            permissionsFingerprint = "permissions:local",
        )
}
