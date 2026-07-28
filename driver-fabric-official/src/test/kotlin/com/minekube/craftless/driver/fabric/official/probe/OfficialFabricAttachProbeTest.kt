package com.minekube.craftless.driver.fabric.official.probe

import com.minekube.craftless.testkit.LocalServerProcessResult
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfficialFabricAttachProbeTest {
    @Test
    fun `official probe cleanup forwards failures to artifacts and stderr`() {
        val artifactsDir = Files.createTempDirectory("craftless-official-probe-cleanup")
        val cleanupFailure = "minecraft server did not stop during teardown"
        var serverStopCalled = false
        var commandStopCalled = false
        val stderr = ByteArrayOutputStream()
        val originalStderr = System.err

        try {
            System.setErr(PrintStream(stderr, true))
            runOfficialProbeCleanup(
                artifactsDir = artifactsDir,
                stopServer = {
                    serverStopCalled = true
                    LocalServerProcessResult(exitCode = 143, evidenceCount = 2, cleanupFailure = cleanupFailure)
                },
                stopCommand = { commandStopCalled = true },
            )
        } finally {
            System.setErr(originalStderr)
        }

        assertTrue(serverStopCalled)
        assertTrue(commandStopCalled)
        assertEquals(
            "$cleanupFailure\n",
            Files.readString(artifactsDir.resolve("server-cleanup.log")),
        )
        assertTrue(stderr.toString().contains(cleanupFailure))
    }
}
