package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.CachePrepareRequest
import com.minekube.craftless.protocol.CachePrepareResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class CachePreparationService(
    workspaceRoot: Path,
) {
    private val root: Path = workspaceRoot.toAbsolutePath().normalize()
    private val json = Json { encodeDefaults = true }

    fun prepare(request: CachePrepareRequest): CachePrepareResult {
        val result = CachePrepareResult.forRequest(request)
        Files.createDirectories(root)
        listOf(
            result.cacheRoot,
            result.minecraftVersionRoot,
            result.loaderRoot,
            result.runtimeRoot,
        ).forEach { handle ->
            Files.createDirectories(resolveHandle(handle))
        }
        val manifest = resolveHandle(result.manifest)
        Files.createDirectories(manifest.parent)
        Files.writeString(manifest, json.encodeToString(result) + "\n")
        return result
    }

    private fun resolveHandle(handle: String): Path {
        require(!Path.of(handle).isAbsolute) { "cache handle must be relative" }
        val resolved = root.resolve(handle).normalize()
        require(resolved.startsWith(root)) { "cache handle must stay under the workspace root" }
        return resolved
    }
}
