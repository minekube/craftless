package com.minekube.craftless.driver.fabric.official.probe

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class OfficialFabricAttachProbeTest {
    @Test
    fun `official probe records cleanup failures in artifacts`() {
        val artifactsDir = Files.createTempDirectory("craftless-official-probe-cleanup")
        val cleanupFailure = "minecraft server did not stop during teardown"

        surfaceOfficialProbeCleanupFailure(artifactsDir, cleanupFailure)

        assertEquals(
            "$cleanupFailure\n",
            Files.readString(artifactsDir.resolve("server-cleanup.log")),
        )
    }
}
