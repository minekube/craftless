package com.minekube.craftless.bridge.hmc

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HmcBridgeBackendTest {
    private fun repositoryRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { path -> path.parent }
            .first { path -> Files.exists(path.resolve("settings.gradle.kts")) }

    @Test
    fun `bridge supports lifecycle actions without exposing hmc command names`() {
        val backend = HmcBridgeBackend.dryRun()
        val connect = backend.connect("alice", "127.0.0.1:25567")
        val stop = backend.stop("alice")

        assertEquals(ClientAction.CONNECT, connect.action)
        assertEquals(ClientAction.STOP, stop.action)
        assertFalse(connect.publicDescription.contains("hmc", ignoreCase = true))
        assertFalse(connect.publicDescription.contains("specifics", ignoreCase = true))
        assertTrue(connect.internalCommand.redacted().contains("<internal bridge command>"))
        assertTrue(stop.internalCommand.redacted().contains("<internal bridge command>"))
    }

    @Test
    fun `bridge backend source has no gameplay helpers`() {
        val source =
            Files.readString(
                repositoryRoot()
                    .resolve("bridge-hmc/src/main/kotlin/com/minekube/craftless/bridge/hmc/HmcBridgeBackend.kt"),
            )

        assertFalse(source.contains("fun chat"))
        assertFalse(source.contains("fun move"))
        assertFalse(source.contains("fun jump"))
        assertFalse(source.contains("fun look"))
        assertFalse(source.contains("MoveIntent"))
        assertFalse(source.contains("ClientAction.CHAT"))
        assertFalse(source.contains("ClientAction.MOVE"))
    }
}
