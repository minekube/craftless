package com.minekube.craftless.testkit

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMinecraftServerSmokeTest {
    @Test
    fun `local server smoke is disabled by default`() {
        val config = LocalMinecraftServerSmokeConfig.fromEnvironment(emptyMap())

        assertFalse(config.enabled)
        assertEquals("latest-release", config.minecraftVersion)
        assertEquals(25565, config.port)
    }

    @Test
    fun `local server smoke config defaults to latest release alias`() {
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = false,
                root = createTempDirectory("craftless-local-server-smoke-default"),
            )

        assertEquals("latest-release", config.minecraftVersion)
    }

    @Test
    fun `local server smoke parses opt in environment`() {
        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf(
                    "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
                    "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to "/tmp/craftless-smoke",
                    "CRAFTLESS_SMOKE_MINECRAFT_VERSION" to "1.21.6",
                    "CRAFTLESS_SMOKE_SERVER_PORT" to "25567",
                    "CRAFTLESS_SMOKE_JAVA_EXECUTABLE" to "/tmp/craftless-java",
                    "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON" to """["/tmp/craftless-action","--target","server"]""",
                    "CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS" to "45000",
                    "CRAFTLESS_SMOKE_EXPECT_PLAYER" to "Alice",
                    "CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE" to "hello from configured smoke command",
                    "CRAFTLESS_SMOKE_EXPECT_DISCONNECT" to "1",
                    "CRAFTLESS_SMOKE_PROVISION_ITEM_ID" to "minecraft:iron_sword",
                    "CRAFTLESS_SMOKE_PROVISION_ITEM_NAME" to "Iron Sword",
                    "CRAFTLESS_SMOKE_PROVISION_ITEM_COUNT" to "1",
                    "CRAFTLESS_SMOKE_READINESS_TIMEOUT_MS" to "90000",
                    "CRAFTLESS_SMOKE_SHUTDOWN_TIMEOUT_MS" to "15000",
                    "CRAFTLESS_SMOKE_MIN_HEAP" to "256M",
                    "CRAFTLESS_SMOKE_MAX_HEAP" to "512M",
                ),
            )

        assertTrue(config.enabled)
        assertEquals(Path.of("/tmp/craftless-smoke"), config.root)
        assertEquals("1.21.6", config.minecraftVersion)
        assertEquals(25567, config.port)
        assertEquals(Path.of("/tmp/craftless-java"), config.javaExecutable)
        assertEquals(listOf("/tmp/craftless-action", "--target", "server"), config.actionCommand)
        assertEquals(45_000, config.actionTimeoutMillis)
        assertEquals("Alice", config.expectedPlayer)
        assertEquals("hello from configured smoke command", config.expectedChatMessage)
        assertTrue(config.expectDisconnect)
        assertEquals(
            LocalMinecraftSmokeProvisionedItem(
                itemId = "minecraft:iron_sword",
                itemName = "Iron Sword",
                count = 1,
            ),
            config.provisionedItem,
        )
        assertEquals(90_000, config.readinessTimeoutMillis)
        assertEquals(15_000, config.shutdownTimeoutMillis)
        assertEquals("256M", config.minHeap)
        assertEquals("512M", config.maxHeap)
    }

    @Test
    fun `local server action command environment removes outer owner variables and keeps child smoke variables`() {
        val env =
            mutableMapOf(
                "PATH" to "/usr/bin",
                "CRAFTLESS_FINAL_GAMEPLAY" to "1",
                "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
                "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to "/tmp/craftless-final-gameplay",
                "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON" to """["gradle",":driver-fabric:runClient"]""",
                "CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE" to "hello from Craftless final gameplay",
                "CRAFTLESS_SMOKE_PROVISION_ITEM_ID" to "minecraft:iron_sword",
                "CRAFTLESS_FABRIC_CLIENT_SMOKE" to "1",
                "CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS" to "120000",
                "CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS" to "1800000",
                "CRAFTLESS_PUBLIC_AGENT_COMMAND_JSON" to """["gradle",":testkit:publicAgentGameplay"]""",
            )

        env.removeInheritedLocalServerSmokeOwnerEnvironment()

        assertEquals("/usr/bin", env["PATH"])
        assertEquals("1", env["CRAFTLESS_FINAL_GAMEPLAY"])
        assertFalse("CRAFTLESS_LOCAL_SERVER_SMOKE" in env)
        assertFalse("CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" in env)
        assertFalse("CRAFTLESS_SMOKE_ACTION_COMMAND_JSON" in env)
        assertFalse("CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE" in env)
        assertFalse("CRAFTLESS_SMOKE_PROVISION_ITEM_ID" in env)
        assertEquals("1", env["CRAFTLESS_FABRIC_CLIENT_SMOKE"])
        assertEquals("120000", env["CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS"])
        assertEquals("1800000", env["CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS"])
        assertEquals("""["gradle",":testkit:publicAgentGameplay"]""", env["CRAFTLESS_PUBLIC_AGENT_COMMAND_JSON"])
    }

    @Test
    fun `local server smoke can consume Java runtime selection from environment`() {
        val root = createTempDirectory("craftless-local-server-smoke-java-selection-env")
        val selectedJava = root.resolve("cache/runtimes/mac-os-arm64/java-runtime-gamma/image/bin/java")
        writeFakeJava(selectedJava)

        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf(
                    "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
                    "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to root.toString(),
                    "CRAFTLESS_SMOKE_JAVA_SELECTION_JSON" to
                        """
                        {
                          "requirement": {
                            "majorVersion": 25,
                            "component": "java-runtime-gamma",
                            "reason": "minecraft-version-metadata"
                          },
                          "status": "SELECTED",
                          "selected": {
                            "id": "managed:25:java",
                            "provider": "MANAGED",
                            "executable": "cache/runtimes/mac-os-arm64/java-runtime-gamma/image/bin/java",
                            "majorVersion": 25,
                            "version": "25.0.3",
                            "managed": true
                          },
                          "reason": "managed-runtime-satisfies-requirement"
                        }
                        """.trimIndent(),
                ),
            )

        assertEquals(selectedJava, config.javaExecutable)
        assertEquals(25, config.javaSelection?.requirement?.majorVersion)
    }

    @Test
    fun `local server smoke records Java runtime selection evidence`() {
        val root = createTempDirectory("craftless-local-server-smoke-java-selection-evidence")
        val selectedJava = root.resolve("cache/runtimes/mac-os-arm64/java-runtime-gamma/image/bin/java")
        val fakeServerJar = root.resolve("server.jar")
        Files.writeString(fakeServerJar, "fake")
        writeFakeJava(selectedJava)
        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf(
                    "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
                    "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to root.toString(),
                    "CRAFTLESS_SMOKE_JAVA_SELECTION_JSON" to
                        """
                        {
                          "requirement": {
                            "majorVersion": 25,
                            "component": "java-runtime-gamma",
                            "reason": "minecraft-version-metadata"
                          },
                          "status": "SELECTED",
                          "selected": {
                            "id": "managed:25:java",
                            "provider": "MANAGED",
                            "executable": "cache/runtimes/mac-os-arm64/java-runtime-gamma/image/bin/java",
                            "majorVersion": 25,
                            "version": "25.0.3",
                            "managed": true
                          },
                          "reason": "managed-runtime-satisfies-requirement"
                        }
                        """.trimIndent(),
                ),
            )

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )

        val evidence = requireNotNull(result.javaSelectionEvidence)
        assertEquals(root.resolve("artifacts").resolve("java-runtime-selection.json"), evidence)
        assertTrue(Files.readString(evidence).contains("\"majorVersion\":25"))
        assertTrue(Files.readString(evidence).contains("java-runtime-gamma"))
    }

    @Test
    fun `local server smoke action command receives resolved Java runtime executable`() {
        val root = createTempDirectory("craftless-local-server-smoke-action-java-selection")
        val selectedJava = root.resolve("cache/runtimes/mac-os-arm64/java-runtime-gamma/image/bin/java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        writeFakeJava(selectedJava)
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            printf '%s\n' "${'$'}CRAFTLESS_SMOKE_JAVA_EXECUTABLE" > action-java-executable.txt
            echo 'configured smoke action saw selected Java'
            """.trimIndent() + "\n",
        )
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf(
                    "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
                    "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to root.toString(),
                    "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON" to """["$actionCommand"]""",
                    "CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS" to "5000",
                    "CRAFTLESS_SMOKE_READINESS_TIMEOUT_MS" to "5000",
                    "CRAFTLESS_SMOKE_SHUTDOWN_TIMEOUT_MS" to "5000",
                    "CRAFTLESS_SMOKE_JAVA_SELECTION_JSON" to
                        """
                        {
                          "requirement": {
                            "majorVersion": 25,
                            "component": "java-runtime-gamma",
                            "reason": "minecraft-version-metadata"
                          },
                          "status": "SELECTED",
                          "selected": {
                            "id": "managed:25:java",
                            "provider": "MANAGED",
                            "executable": "cache/runtimes/mac-os-arm64/java-runtime-gamma/image/bin/java",
                            "majorVersion": 25,
                            "version": "25.0.3",
                            "managed": true
                          },
                          "reason": "managed-runtime-satisfies-requirement"
                        }
                        """.trimIndent(),
                ),
            )

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )

        assertEquals(LocalMinecraftServerSmokeStatus.RAN, result.status)
        assertEquals(selectedJava.toString() + "\n", Files.readString(root.resolve("action-java-executable.txt")))
        assertTrue(Files.readString(requireNotNull(result.actionLog)).contains("configured smoke action saw selected Java"))
    }

    @Test
    fun `local server smoke records unsupported runtime lane without provisioning server`() {
        val root = createTempDirectory("craftless-local-server-smoke-runtime-lane")
        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf(
                    "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
                    "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to root.toString(),
                    "CRAFTLESS_SMOKE_RUNTIME_LANE_JSON" to
                        """
                        {
                          "id": "fabric-unsupported-26-2",
                          "status": "UNSUPPORTED",
                          "minecraftVersion": "26.2",
                          "javaMajorVersion": 25,
                          "providerId": "fabric-unsupported",
                          "unsupportedReason": "unsupported-version"
                        }
                        """.trimIndent(),
                ),
            )

        var provisioned = false
        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ ->
                    provisioned = true
                    root.resolve("server.jar")
                },
            )

        assertEquals(LocalMinecraftServerSmokeStatus.UNSUPPORTED, result.status)
        assertFalse(provisioned)
        val evidence = requireNotNull(result.runtimeLaneEvidence)
        assertEquals(root.resolve("artifacts").resolve("runtime-lane.json"), evidence)
        assertTrue(Files.readString(evidence).contains("unsupported-version"))
    }

    @Test
    fun `local server smoke is enabled by fabric client smoke gate`() {
        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf("CRAFTLESS_FABRIC_CLIENT_SMOKE" to "1"),
            )

        assertTrue(config.enabled)
    }

    @Test
    fun `fabric client smoke chooses non-default port when none is configured`() {
        val config =
            LocalMinecraftServerSmokeConfig.fromEnvironment(
                mapOf("CRAFTLESS_FABRIC_CLIENT_SMOKE" to "1"),
            )

        assertTrue(config.port in 1..65535)
        assertFalse(config.port == 25565)
    }

    @Test
    fun `local server smoke skips without opt in`() {
        val result =
            LocalMinecraftServerSmoke.run(
                config = LocalMinecraftServerSmokeConfig.fromEnvironment(emptyMap()),
            )

        assertEquals(LocalMinecraftServerSmokeStatus.SKIPPED, result.status)
        assertEquals("set CRAFTLESS_LOCAL_SERVER_SMOKE=1 to run the local server smoke", result.message)
    }

    @Test
    fun `local server smoke keeps server running while caller action executes`() {
        val root = createTempDirectory("craftless-local-server-smoke-action")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            touch server-is-ready
            read command
            printf '%s\n' "${'$'}command" > minecraft-server-stdin.txt
            echo '[12:00:01] [Server thread/INFO]: Alice joined the game'
            echo '[12:00:02] [Server thread/INFO]: <Alice> hello while smoke action ran'
            echo '[12:00:03] [Server thread/INFO]: Alice left the game'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )
        var actionObservedRunningServer = false

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            ) { layout, handle ->
                actionObservedRunningServer = handle.isRunning()
                assertTrue(waitUntilExists(root.resolve("server-is-ready")))
                assertFalse(Files.exists(root.resolve("minecraft-server-stdin.txt")))
                assertTrue(Files.exists(layout.serverProperties))
            }

        assertTrue(actionObservedRunningServer)
        assertEquals(LocalMinecraftServerSmokeStatus.RAN, result.status)
        assertEquals(3, result.evidenceCount)
        assertEquals("stop\n", Files.readString(root.resolve("minecraft-server-stdin.txt")))
    }

    @Test
    fun `local server smoke runs configured command before stopping server`() {
        val root = createTempDirectory("craftless-local-server-smoke-command")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.createDirectories(root.resolve("artifacts"))
        Files.writeString(root.resolve("artifacts").resolve("stale-result.json"), "old")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            touch server-is-ready
            read command
            printf '%s\n' "${'$'}command" > minecraft-server-stdin.txt
            echo '[12:00:01] [Server thread/INFO]: Alice joined the game'
            echo '[12:00:02] [Server thread/INFO]: <Alice> hello from configured smoke command'
            echo '[12:00:03] [Server thread/INFO]: Alice left the game'
            """.trimIndent() + "\n",
        )
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            test -f server-is-ready
            test ! -f minecraft-server-stdin.txt
            printf '%s\n' "${'$'}PWD" > action-working-directory.txt
            printf '%s\n' "$@" > action-arguments.txt
            printf '%s\n' "${'$'}CRAFTLESS_SMOKE_SERVER_PORT" > action-server-port.txt
            printf '%s\n' "${'$'}CRAFTLESS_SMOKE_ARTIFACTS_DIR" > action-artifacts-dir.txt
            echo 'configured smoke action ran'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                port = 25567,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString(), "--client", "fabric"),
                actionTimeoutMillis = 5_000,
                expectedPlayer = "Alice",
                expectedChatMessage = "hello from configured smoke command",
                expectDisconnect = true,
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )

        assertEquals(LocalMinecraftServerSmokeStatus.RAN, result.status)
        assertEquals(0, result.actionExitCode)
        assertEquals(
            root.toRealPath(),
            Path.of(Files.readString(root.resolve("action-working-directory.txt")).trim()).toRealPath(),
        )
        assertEquals(listOf("--client", "fabric"), Files.readAllLines(root.resolve("action-arguments.txt")))
        assertEquals("25567\n", Files.readString(root.resolve("action-server-port.txt")))
        assertFalse(Files.exists(root.resolve("artifacts").resolve("stale-result.json")))
        assertEquals(
            root.resolve("artifacts").toRealPath(),
            Path.of(Files.readString(root.resolve("action-artifacts-dir.txt")).trim()).toRealPath(),
        )
        assertEquals("stop\n", Files.readString(root.resolve("minecraft-server-stdin.txt")))
        assertTrue(Files.readString(requireNotNull(result.actionLog)).contains("configured smoke action ran"))
        assertEquals(
            LocalMinecraftSmokeEvidenceSummary(
                playerJoined = true,
                chatObserved = true,
                playerDisconnected = true,
            ),
            result.evidenceSummary,
        )
    }

    @Test
    fun `local server smoke provisions configured target item while action command runs`() {
        val root = createTempDirectory("craftless-local-server-smoke-provision-item")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            touch server-is-ready
            echo '[12:00:01] [Server thread/INFO]: Alice joined the game'
            read give_command
            printf '%s\n' "${'$'}give_command" > minecraft-server-give-stdin.txt
            echo '[12:00:02] [Server thread/INFO]: Gave 1 [Iron Sword] to Alice'
            read stop_command
            printf '%s\n' "${'$'}stop_command" > minecraft-server-stop-stdin.txt
            echo '[12:00:03] [Server thread/INFO]: Alice left the game'
            """.trimIndent() + "\n",
        )
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            test -f server-is-ready
            echo 'configured smoke action ran while item was provisioned'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                port = 25567,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString()),
                actionTimeoutMillis = 5_000,
                expectedPlayer = "Alice",
                expectDisconnect = true,
                provisionedItem =
                    LocalMinecraftSmokeProvisionedItem(
                        itemId = "minecraft:iron_sword",
                        itemName = "Iron Sword",
                        count = 1,
                    ),
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )

        assertEquals(LocalMinecraftServerSmokeStatus.RAN, result.status)
        assertEquals("give Alice minecraft:iron_sword 1\n", Files.readString(root.resolve("minecraft-server-give-stdin.txt")))
        assertEquals("stop\n", Files.readString(root.resolve("minecraft-server-stop-stdin.txt")))
        assertEquals(
            LocalMinecraftSmokeEvidenceSummary(
                playerJoined = true,
                playerDisconnected = true,
                targetItemProvisioned = true,
            ),
            result.evidenceSummary,
        )
        assertTrue(Files.readString(requireNotNull(result.evidenceLog)).contains("ITEM_PROVISIONED"))
        assertTrue(Files.readString(requireNotNull(result.actionLog)).contains("configured smoke action ran"))
    }

    @Test
    fun `local server smoke provisions configured target item for first observed player`() {
        val root = createTempDirectory("craftless-local-server-smoke-provision-first-player")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            echo '[12:00:01] [Server thread/INFO]: Player840 joined the game'
            read give_command
            printf '%s\n' "${'$'}give_command" > minecraft-server-give-stdin.txt
            read stop_command
            printf '%s\n' "${'$'}stop_command" > minecraft-server-stop-stdin.txt
            """.trimIndent() + "\n",
        )
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            echo 'configured smoke action ran'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString()),
                actionTimeoutMillis = 5_000,
                provisionedItem =
                    LocalMinecraftSmokeProvisionedItem(
                        itemId = "minecraft:iron_sword",
                        itemName = "Iron Sword",
                    ),
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )

        assertEquals("give Player840 minecraft:iron_sword 1\n", Files.readString(root.resolve("minecraft-server-give-stdin.txt")))
        assertTrue(result.evidenceSummary.targetItemProvisioned)
        assertTrue(Files.readString(requireNotNull(result.evidenceLog)).contains("\"player\":\"Player840\""))
    }

    @Test
    fun `local server smoke writes action log when provisioning cannot observe a joined player`() {
        val root = createTempDirectory("craftless-local-server-smoke-provision-missing-join")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            read stop_command
            printf '%s\n' "${'$'}stop_command" > minecraft-server-stop-stdin.txt
            """.trimIndent() + "\n",
        )
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            echo 'action started before join timeout'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString()),
                actionTimeoutMillis = 250,
                provisionedItem =
                    LocalMinecraftSmokeProvisionedItem(
                        itemId = "minecraft:iron_sword",
                        itemName = "Iron Sword",
                    ),
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        val error =
            assertFailsWith<IllegalStateException> {
                LocalMinecraftServerSmoke.runWithServer(
                    config = config,
                    provisionServerJar = { _, _ -> fakeServerJar },
                )
            }

        assertTrue(error.message?.contains("actionLog=") == true)
        assertTrue(Files.readString(root.resolve("artifacts").resolve("smoke-action.log")).contains("action started"))
    }

    @Test
    fun `local server smoke fails when expected chat evidence is missing`() {
        val root = createTempDirectory("craftless-local-server-smoke-missing-evidence")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            read command
            printf '%s\n' "${'$'}command" > minecraft-server-stdin.txt
            echo '[12:00:01] [Server thread/INFO]: Alice joined the game'
            echo '[12:00:02] [Server thread/INFO]: Alice left the game'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                expectedPlayer = "Alice",
                expectedChatMessage = "hello missing",
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        val error =
            assertFailsWith<IllegalStateException> {
                LocalMinecraftServerSmoke.runWithServer(
                    config = config,
                    provisionServerJar = { _, _ -> fakeServerJar },
                )
            }

        assertTrue(error.message?.contains("expected chat evidence") == true)
        assertEquals("stop\n", Files.readString(root.resolve("minecraft-server-stdin.txt")))
    }

    @Test
    fun `a server that refuses to stop cannot turn a passing product run red`() {
        // Reproduces the 2026-07-27 canary: every product assertion passed and the run
        // was still marked red because the Minecraft server outlived its shutdown budget.
        val root = createTempDirectory("craftless-local-server-smoke-teardown-cannot-invert")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        Files.writeString(
            fakeJava,
            """
            #!/bin/sh
            echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
            echo '[12:00:01] [Server thread/INFO]: LatestCurrent joined the game'
            echo '[12:00:02] [Server thread/INFO]: LatestCurrent left the game'
            exec sleep 30
            """.trimIndent() + "\n",
        )
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            echo 'probe reached PLAYER_JOINED and every generated assertion passed'
            """.trimIndent() + "\n",
        )
        assertTrue(fakeJava.toFile().setExecutable(true))
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString()),
                actionTimeoutMillis = 10_000,
                expectedPlayer = "LatestCurrent",
                expectDisconnect = true,
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 1_000,
            )

        val result =
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )

        assertEquals(LocalMinecraftServerSmokeStatus.RAN, result.status)
        assertTrue(result.evidenceSummary.playerJoined)
        assertTrue(result.evidenceSummary.playerDisconnected)

        // The cleanup failure is recorded, not swallowed, and not treated as a verdict.
        val cleanupFailure = requireNotNull(result.cleanupFailure)
        assertTrue(cleanupFailure.contains("did not stop after 1000ms"), cleanupFailure)

        val outcome = requireNotNull(result.outcome)
        assertEquals(CanaryVerdict.PASS, outcome.verdict)
        assertNull(outcome.failureClass)
        assertEquals(cleanupFailure, outcome.cleanupFailure)
        assertEquals(outcome, CanaryOutcome.read(root.resolve("artifacts").resolve(CanaryOutcome.FILE_NAME)))
    }

    @Test
    fun `a failing product assertion is recorded as a product failure`() {
        val root = createTempDirectory("craftless-local-server-smoke-product-failure")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        writeFakeJava(fakeJava)
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            echo 'generated action world.time.query did not return ACCEPTED' >&2
            exit 1
            """.trimIndent() + "\n",
        )
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString()),
                actionTimeoutMillis = 10_000,
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        assertFailsWith<IllegalStateException> {
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )
        }

        val outcome = CanaryOutcome.read(root.resolve("artifacts").resolve(CanaryOutcome.FILE_NAME))
        assertEquals(CanaryVerdict.FAIL, outcome.verdict)
        assertEquals(CanaryFailureClass.PRODUCT, outcome.failureClass)
        assertEquals(CanaryPhase.PRODUCT, outcome.phase)
        assertNull(outcome.signature)
    }

    @Test
    fun `an upstream cdn timeout is recorded as an infrastructure failure`() {
        // Reproduces the 2026-07-28 canary: the probe failed at create-client because the
        // Mojang asset CDN did not answer. Same exit path as the product failure above,
        // and it must not read as "the supported Minecraft lane broke".
        val root = createTempDirectory("craftless-local-server-smoke-infrastructure-failure")
        val fakeJava = root.resolve("fake-java")
        val fakeServerJar = root.resolve("server.jar")
        val actionCommand = root.resolve("smoke-action")
        Files.writeString(fakeServerJar, "fake")
        writeFakeJava(fakeJava)
        Files.writeString(
            actionCommand,
            """
            #!/bin/sh
            printf '%s\n' '{"code":"BAD_REQUEST","message":"Connect timeout has expired [url=https://resources.download.minecraft.net/f0/f066613bca60316fdc9ae333e39c1c9fd8a06e4de, connect_timeout=15000 ms]"}' \
              > "${'$'}CRAFTLESS_SMOKE_ARTIFACTS_DIR/clients-create-latest-release.log"
            exit 1
            """.trimIndent() + "\n",
        )
        assertTrue(actionCommand.toFile().setExecutable(true))
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                javaExecutable = fakeJava,
                actionCommand = listOf(actionCommand.toString()),
                actionTimeoutMillis = 10_000,
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        assertFailsWith<IllegalStateException> {
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> fakeServerJar },
            )
        }

        val outcome = CanaryOutcome.read(root.resolve("artifacts").resolve(CanaryOutcome.FILE_NAME))
        assertEquals(CanaryVerdict.FAIL, outcome.verdict)
        assertEquals(CanaryFailureClass.INFRASTRUCTURE, outcome.failureClass)
        assertEquals("upstream-connect-timeout", outcome.signature)
    }

    @Test
    fun `harness setup failure is never reported as a product failure`() {
        val root = createTempDirectory("craftless-local-server-smoke-setup-failure")
        val config =
            LocalMinecraftServerSmokeConfig(
                enabled = true,
                root = root,
                readinessTimeoutMillis = 5_000,
                shutdownTimeoutMillis = 5_000,
            )

        assertFailsWith<IllegalStateException> {
            LocalMinecraftServerSmoke.runWithServer(
                config = config,
                provisionServerJar = { _, _ -> error("could not provision the harness server jar") },
            )
        }

        val outcome = CanaryOutcome.read(root.resolve("artifacts").resolve(CanaryOutcome.FILE_NAME))
        assertEquals(CanaryVerdict.FAIL, outcome.verdict)
        assertEquals(CanaryFailureClass.INFRASTRUCTURE, outcome.failureClass)
        assertEquals(CanaryPhase.SETUP, outcome.phase)
    }
}

private fun waitUntilExists(
    path: Path,
    timeoutMillis: Long = 1_000,
): Boolean {
    val deadline = System.nanoTime() + timeoutMillis * 1_000_000
    while (System.nanoTime() < deadline) {
        if (Files.exists(path)) {
            return true
        }
        Thread.sleep(10)
    }
    return Files.exists(path)
}

private fun writeFakeJava(path: Path) {
    Files.createDirectories(path.parent)
    Files.writeString(
        path,
        """
        #!/bin/sh
        echo '[12:00:00] [Server thread/INFO]: Done (1.000s)! For help, type "help"'
        read command
        printf '%s\n' "${'$'}command" > minecraft-server-stdin.txt
        """.trimIndent() + "\n",
    )
    assertTrue(path.toFile().setExecutable(true))
}
