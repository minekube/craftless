package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.JavaRuntimeDescriptor
import com.minekube.craftless.protocol.JavaRuntimeListResult
import com.minekube.craftless.protocol.JavaRuntimeResolveRequest
import com.minekube.craftless.protocol.JavaRuntimeSelection
import com.minekube.craftless.protocol.MINECRAFT_VERSION_INDEX_URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                ?: MinecraftJavaRuntimeRequirementResolver().derive(
                    versionManifest = versionManifest(requireNotNull(request.minecraftVersion)),
                    minecraftVersion = requireNotNull(request.minecraftVersion),
                )
        return resolver.resolve(requirement, discoveryContext()).withWorkspaceHandles()
    }

    private fun discoveryContext(): JavaRuntimeDiscoveryContext =
        JavaRuntimeDiscoveryContext(
            managedRuntimeRoot = root.resolve("cache/runtimes"),
        )

    private suspend fun versionManifest(minecraftVersion: String): String {
        val versionIndex = metadataFetcher.fetchText(MINECRAFT_VERSION_INDEX_URL)
        val versionManifestUrl = versionIndex.versionManifestUrl(minecraftVersion)
        return metadataFetcher.fetchText(versionManifestUrl)
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

private fun String.versionManifestUrl(minecraftVersion: String): String {
    val versions =
        Json
            .parseToJsonElement(this)
            .jsonObject["versions"]
            ?.jsonArray
            .orEmpty()
    val version =
        versions.firstOrNull { version ->
            version.jsonObject["id"]?.jsonPrimitive?.content == minecraftVersion
        } ?: error("minecraft version $minecraftVersion was not found in version index")
    return version.jsonObject["url"]?.jsonPrimitive?.content ?: error("minecraft version $minecraftVersion is missing metadata url")
}
