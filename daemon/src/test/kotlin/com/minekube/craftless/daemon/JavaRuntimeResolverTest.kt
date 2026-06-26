package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.JavaRuntimeProviderKind
import com.minekube.craftless.protocol.JavaRuntimeRequirement
import com.minekube.craftless.protocol.JavaRuntimeSelectionStatus
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaRuntimeResolverTest {
    @Test
    fun `selects configured Java before lower system Java`() {
        val configuredJava = fakeJava("configured-25", "25.0.3")
        val systemJava = fakeJava("system-21", "21.0.11")

        val selection =
            JavaRuntimeResolver()
                .resolve(
                    requirement = java25Requirement(),
                    context =
                        JavaRuntimeDiscoveryContext(
                            configuredExecutables = listOf(configuredJava),
                            systemExecutables = listOf(systemJava),
                        ),
                )

        assertEquals(JavaRuntimeSelectionStatus.SELECTED, selection.status)
        assertEquals(JavaRuntimeProviderKind.CONFIGURED, selection.selected?.provider)
        assertEquals(configuredJava.toString(), selection.selected?.executable)
        assertTrue(selection.rejected.any { it.provider == JavaRuntimeProviderKind.SYSTEM && it.reason == "java-major-too-low" })
    }

    @Test
    fun `selects managed cache Java when no configured runtime is present`() {
        val root = Files.createTempDirectory("craftless-managed-java")
        val managedJava = root.resolve("mac-os-arm64/java-runtime-gamma/image/bin/java")
        fakeJava(managedJava, "25.0.3")

        val selection =
            JavaRuntimeResolver()
                .resolve(
                    requirement = java25Requirement(),
                    context = JavaRuntimeDiscoveryContext(managedRuntimeRoot = root),
                )

        assertEquals(JavaRuntimeSelectionStatus.SELECTED, selection.status)
        assertEquals(JavaRuntimeProviderKind.MANAGED, selection.selected?.provider)
        assertEquals(managedJava.toString(), selection.selected?.executable)
        assertEquals(true, selection.selected?.managed)
    }

    @Test
    fun `discovers mise Java installations without requiring mise on PATH`() {
        val miseData = Files.createTempDirectory("craftless-mise-data")
        val miseJava = miseData.resolve("installs/java/temurin-25.0.3/bin/java")
        fakeJava(miseJava, "25.0.3")

        val selection =
            JavaRuntimeResolver()
                .resolve(
                    requirement = java25Requirement(),
                    context =
                        JavaRuntimeDiscoveryContext(
                            environment = mapOf("MISE_DATA_DIR" to miseData.toString()),
                            home = Files.createTempDirectory("craftless-home"),
                        ),
                )

        assertEquals(JavaRuntimeSelectionStatus.SELECTED, selection.status)
        assertEquals(JavaRuntimeProviderKind.MISE, selection.selected?.provider)
        assertEquals(miseJava.toString(), selection.selected?.executable)
    }

    @Test
    fun `returns unsatisfied when every candidate is too old`() {
        val systemJava = fakeJava("system-21", "21.0.11")

        val selection =
            JavaRuntimeResolver()
                .resolve(
                    requirement = java25Requirement(),
                    context =
                        JavaRuntimeDiscoveryContext(
                            environment = emptyMap(),
                            home = Files.createTempDirectory("craftless-empty-home"),
                            systemExecutables = listOf(systemJava),
                        ),
                )

        assertEquals(JavaRuntimeSelectionStatus.UNSATISFIED, selection.status)
        assertEquals(null, selection.selected)
        assertEquals("java-runtime.unsatisfied", selection.reason)
        assertTrue(selection.rejected.any { it.reason == "java-major-too-low" && it.detectedMajorVersion == 21 })
    }

    private fun java25Requirement(): JavaRuntimeRequirement =
        JavaRuntimeRequirement(
            majorVersion = 25,
            component = "java-runtime-gamma",
            reason = "minecraft-version-metadata",
        )

    private fun fakeJava(
        name: String,
        version: String,
    ): Path {
        val directory = Files.createTempDirectory("craftless-$name")
        return fakeJava(directory.resolve("bin/java"), version)
    }

    private fun fakeJava(
        path: Path,
        version: String,
    ): Path {
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            """
            #!/usr/bin/env sh
            echo 'openjdk version "$version" 2026-04-21 LTS' >&2
            echo 'Eclipse Temurin Runtime Environment' >&2
            echo '    os.arch = aarch64' >&2
            """.trimIndent() + "\n",
        )
        path.toFile().setExecutable(true, true)
        return path
    }
}
