package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.driver.fabric.runtime.FabricCompiledLaneMetadata
import com.minekube.craftless.driver.fabric.runtime.FabricRuntimeIdentity
import com.minekube.craftless.driver.fabric.runtime.FabricRuntimeSupportState
import com.minekube.craftless.driver.fabric.runtime.selectFabricRuntimeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FabricCurrentLaneRuntimeProviderTest {
    @Test
    fun `current compiled lane provider supports the matching runtime identity`() {
        val provider = FabricCurrentLaneRuntimeProvider()
        val identity = currentLaneIdentity()

        val selection = selectFabricRuntimeProvider(identity, listOf(provider))

        assertEquals(FabricCompiledLaneMetadata.PROVIDER_ID, provider.id)
        assertSame(provider, selection.provider)
        assertEquals(FabricRuntimeSupportState.SUPPORTED, selection.support.state)
        assertEquals("supported", selection.support.reason)
        assertEquals(identity, selection.access.identity)
    }

    @Test
    fun `current compiled lane provider rejects mismatched runtime identity`() {
        val provider = FabricCurrentLaneRuntimeProvider()
        val identity = currentLaneIdentity().copy(gameVersion = "26.2")

        val support = provider.support(identity)

        assertEquals(FabricRuntimeSupportState.UNSUPPORTED, support.state)
        assertEquals("unsupported-version", support.reason)
    }

    private fun currentLaneIdentity(): FabricRuntimeIdentity =
        FabricRuntimeIdentity(
            gameVersion = FabricCompiledLaneMetadata.MINECRAFT_VERSION,
            loaderVersion = FabricCompiledLaneMetadata.LOADER_VERSION,
            fabricApiVersion = FabricCompiledLaneMetadata.FABRIC_API_VERSION,
            mappingsFingerprint = "mappings:current-lane",
            installedModsFingerprint = "mods:current-lane",
            registryFingerprint = "registries:current-lane",
            serverFeatureFingerprint = "server-features:local",
            permissionsFingerprint = "permissions:local",
        )
}
