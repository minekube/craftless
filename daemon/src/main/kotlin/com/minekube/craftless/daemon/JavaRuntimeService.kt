package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.JavaRuntimeDescriptor
import com.minekube.craftless.protocol.JavaRuntimeListResult
import com.minekube.craftless.protocol.JavaRuntimeRequirement
import com.minekube.craftless.protocol.JavaRuntimeResolveRequest
import com.minekube.craftless.protocol.JavaRuntimeSelection
import com.minekube.craftless.protocol.MINECRAFT_VERSION_INDEX_URL
import com.minekube.craftless.protocol.resolveMinecraftVersion
import com.minekube.craftless.protocol.versionManifestUrl
import java.nio.file.Path

class JavaRuntimeService(
    workspaceRoot: Path,
    private val metadataFetcher: CacheMetadataFetcher = KtorCacheMetadataFetcher(),
    private val resolver: JavaRuntimeResolver = JavaRuntimeResolver(),
) {
    private val root = workspaceRoot.toAbsolutePath().normalize()

    fun list(): JavaRuntimeListResult = resolver.list(discoveryContext()).withWorkspaceHandles()

    suspend fun resolve(request: JavaRuntimeResolveRequest): JavaRuntimeSelection {
        val requirement =
            request.requirement
                ?: javaRuntimeRequirement(requireNotNull(request.minecraftVersion))
        return resolver.resolve(requirement, discoveryContext()).withWorkspaceHandles()
    }

    private fun discoveryContext(): JavaRuntimeDiscoveryContext =
        JavaRuntimeDiscoveryContext(
            managedRuntimeRoot = root.resolve("cache/runtimes"),
        )

    private suspend fun javaRuntimeRequirement(minecraftVersion: String): JavaRuntimeRequirement {
        val versionIndex = metadataFetcher.fetchText(MINECRAFT_VERSION_INDEX_URL)
        val resolvedMinecraftVersion = versionIndex.resolveMinecraftVersion(minecraftVersion)
        val versionManifestUrl = versionIndex.versionManifestUrl(resolvedMinecraftVersion)
        return MinecraftJavaRuntimeRequirementResolver().derive(
            versionManifest = metadataFetcher.fetchText(versionManifestUrl),
            minecraftVersion = resolvedMinecraftVersion,
        )
    }

    private fun JavaRuntimeListResult.withWorkspaceHandles(): JavaRuntimeListResult =
        copy(runtimes = runtimes.map { descriptor -> descriptor.withWorkspaceHandles() })

    private fun JavaRuntimeSelection.withWorkspaceHandles(): JavaRuntimeSelection =
        selected
            ?.let { descriptor ->
                copy(selected = descriptor.withWorkspaceHandles())
            }
            ?: this

    private fun JavaRuntimeDescriptor.withWorkspaceHandles(): JavaRuntimeDescriptor =
        copy(
            javaHome = javaHome?.let(::cacheHandleOrPath),
            executable = cacheHandleOrPath(executable),
        )

    private fun cacheHandleOrPath(value: String): String {
        val path = Path.of(value)
        if (!path.isAbsolute) return value
        val normalized = path.normalize()
        return if (normalized.startsWith(root)) {
            root.relativize(normalized).toString().replace('\\', '/')
        } else {
            value
        }
    }
}
