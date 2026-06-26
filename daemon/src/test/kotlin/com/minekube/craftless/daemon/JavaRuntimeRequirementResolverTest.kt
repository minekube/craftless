package com.minekube.craftless.daemon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JavaRuntimeRequirementResolverTest {
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
    fun `uses Java 8 fallback only for legacy manifests without metadata`() {
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
}
