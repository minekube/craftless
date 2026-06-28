package com.minekube.craftless.daemon

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JavaRuntimeRequirementResolverTest {
    @Test
    fun `metadata fallback internals use pre metadata naming`() {
        val root = repositoryRoot()
        val files =
            listOf(
                "daemon/src/main/kotlin/com/minekube/craftless/daemon/MinecraftJavaRuntimeRequirementResolver.kt",
                "daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt",
                "daemon/src/test/kotlin/com/minekube/craftless/daemon/JavaRuntimeRequirementResolverTest.kt",
            )
        val forbidden =
            listOf(
                "is" + "LegacyMinecraftVersion",
                "nativeFrom" + "Legacy",
                "legacy " + "manifests",
                "legacy " + "manifest",
            )
        val violations =
            files
                .flatMap { relative ->
                    val contents = Files.readString(root.resolve(relative))
                    forbidden
                        .filter { token -> contents.contains(token, ignoreCase = true) }
                        .map { token -> "$relative: $token" }
                }

        assertTrue(
            violations.isEmpty(),
            "Daemon metadata fallback internals must use pre-metadata/classifier wording:\n" +
                violations.joinToString("\n"),
        )
    }

    @Test
    fun `derives Java 25 from Minecraft 26 metadata`() {
        val manifest =
            """
            {
              "id": "26.2",
              "javaVersion": {
                "component": "java-runtime-gamma",
                "majorVersion": 25
              }
            }
            """.trimIndent()

        val requirement = MinecraftJavaRuntimeRequirementResolver().derive(manifest, "26.2")

        assertEquals(25, requirement.majorVersion)
        assertEquals("java-runtime-gamma", requirement.component)
        assertEquals("minecraft-version-metadata", requirement.reason)
    }

    @Test
    fun `uses Java 8 fallback only for manifests before Java runtime metadata`() {
        val manifest =
            """
            {
              "id": "1.7.10"
            }
            """.trimIndent()

        val requirement = MinecraftJavaRuntimeRequirementResolver().derive(manifest, "1.7.10")

        assertEquals(8, requirement.majorVersion)
        assertEquals(null, requirement.component)
        assertEquals("minecraft-version-metadata-missing", requirement.reason)
    }

    @Test
    fun `rejects modern manifests without Java metadata`() {
        val manifest =
            """
            {
              "id": "26.2"
            }
            """.trimIndent()

        val error =
            assertFailsWith<IllegalArgumentException> {
                MinecraftJavaRuntimeRequirementResolver().derive(manifest, "26.2")
            }

        assertEquals("minecraft version 26.2 is missing Java runtime metadata", error.message)
    }

    private fun repositoryRoot(): Path {
        var current = Path.of("").toAbsolutePath().normalize()
        while (!Files.exists(current.resolve("settings.gradle.kts"))) {
            current = requireNotNull(current.parent) { "repository root not found" }
        }
        return current
    }
}
