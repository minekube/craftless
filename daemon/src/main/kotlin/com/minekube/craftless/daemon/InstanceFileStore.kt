package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.InstanceFiles
import java.nio.file.Files
import java.nio.file.Path

class InstanceFileStore(
    workspaceRoot: Path,
) {
    private val root: Path = workspaceRoot.toAbsolutePath().normalize()

    fun prepare(files: InstanceFiles): InstanceFileLayout {
        Files.createDirectories(root)
        val directories = files.directoryHandles().map(::resolveHandle)
        directories.forEach(Files::createDirectories)
        return InstanceFileLayout(root, directories)
    }

    private fun resolveHandle(handle: String): Path {
        require(!Path.of(handle).isAbsolute) { "instance file handle must be relative" }
        val resolved = root.resolve(handle).normalize()
        require(resolved.startsWith(root)) { "instance file handle must stay under the workspace root" }
        return resolved
    }
}

data class InstanceFileLayout(
    val workspaceRoot: Path,
    val directories: List<Path>,
)

internal fun InstanceFiles.directoryHandles(): List<String> =
    listOf(
        root,
        gameRoot,
        runtimeRoot,
        cache,
        mods,
        config,
        saves,
        resourcePacks,
        shaderPacks,
        screenshots,
        logs,
        artifacts,
    )
