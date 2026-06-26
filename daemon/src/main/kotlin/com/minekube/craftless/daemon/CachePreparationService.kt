package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.CacheLaunchPlan
import com.minekube.craftless.protocol.CachePrepareRequest
import com.minekube.craftless.protocol.CachePrepareResult
import com.minekube.craftless.protocol.CachePreparedArtifact
import com.minekube.craftless.protocol.CachePreparedArtifactKind
import com.minekube.craftless.protocol.CachePreparedArtifactStatus
import com.minekube.craftless.protocol.FABRIC_META_BASE_URL
import com.minekube.craftless.protocol.Loader
import com.minekube.craftless.protocol.MINECRAFT_VERSION_INDEX_URL
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class CachePreparationService(
    workspaceRoot: Path,
    private val metadataFetcher: CacheMetadataFetcher = KtorCacheMetadataFetcher(),
) {
    private val root: Path = workspaceRoot.toAbsolutePath().normalize()
    private val json = Json { encodeDefaults = true }

    suspend fun prepare(request: CachePrepareRequest): CachePrepareResult {
        val versionIndex = metadataFetcher.fetchText(MINECRAFT_VERSION_INDEX_URL)
        val versionManifestUrl = versionIndex.versionManifestUrl(request.minecraftVersion)
        val versionManifest = metadataFetcher.fetchText(versionManifestUrl)
        val clientJarUrl = versionManifest.clientJarUrl(request.minecraftVersion)
        val assetIndexMetadata = versionManifest.assetIndexMetadata(request.minecraftVersion)
        val assetIndex = metadataFetcher.fetchText(assetIndexMetadata.url)
        val assetObjects = assetIndex.assetObjects()
        val fabricMetadata = resolveFabricMetadata(request)
        val fabricLibraries = fabricMetadata?.profile?.fabricLibraries().orEmpty()
        val baseResult = CachePrepareResult.forRequest(request, fabricMetadata?.loaderVersion)
        val artifacts =
            baseResult.artifacts
                .map { artifact ->
                    when (artifact.kind) {
                        CachePreparedArtifactKind.MINECRAFT_VERSION_MANIFEST -> artifact.copy(source = versionManifestUrl)
                        CachePreparedArtifactKind.MINECRAFT_CLIENT_JAR -> artifact.copy(source = clientJarUrl)
                        CachePreparedArtifactKind.MINECRAFT_ASSET_INDEX -> artifact.copy(source = assetIndexMetadata.url)
                        CachePreparedArtifactKind.FABRIC_LOADER_PROFILE -> artifact.copy(source = fabricMetadata?.profileUrl)
                        else -> artifact
                    }
                } + assetObjects.map { it.artifact } + fabricLibraries.map { it.artifact }
        val result =
            baseResult.copy(
                artifacts = artifacts,
                launch = CacheLaunchPlan.fromArtifacts(artifacts),
            )
        Files.createDirectories(root)
        listOf(
            result.cacheRoot,
            result.minecraftVersionRoot,
            result.loaderRoot,
            result.runtimeRoot,
        ).forEach { handle ->
            Files.createDirectories(resolveHandle(handle))
        }
        writeTextArtifact(
            result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_VERSION_INDEX },
            versionIndex,
        )
        writeTextArtifact(
            result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_VERSION_MANIFEST },
            versionManifest,
        )
        writeBytesArtifact(
            result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_CLIENT_JAR },
            metadataFetcher.fetchBytes(clientJarUrl),
        )
        writeTextArtifact(
            result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_ASSET_INDEX },
            assetIndex,
        )
        fabricMetadata?.let { metadata ->
            writeTextArtifact(
                result.artifacts.single { it.kind == CachePreparedArtifactKind.FABRIC_LOADER_VERSIONS },
                metadata.loaderVersions,
            )
            writeTextArtifact(
                result.artifacts.single { it.kind == CachePreparedArtifactKind.FABRIC_LOADER_PROFILE },
                metadata.profile,
            )
        }
        assetObjects.forEach { asset ->
            writeBytesArtifact(asset.artifact, metadataFetcher.fetchBytes(asset.source))
        }
        fabricLibraries.forEach { library ->
            writeBytesArtifact(library.artifact, metadataFetcher.fetchBytes(library.source))
        }
        val manifest = resolveHandle(result.manifest)
        Files.createDirectories(manifest.parent)
        Files.writeString(manifest, json.encodeToString(result) + "\n")
        return result
    }

    private suspend fun resolveFabricMetadata(request: CachePrepareRequest): FabricCacheMetadata? {
        if (request.loader != Loader.FABRIC) return null
        val loaderVersionsUrl = fabricLoaderVersionsUrl(request.minecraftVersion)
        val loaderVersions = metadataFetcher.fetchText(loaderVersionsUrl)
        val loaderVersion = loaderVersions.compatibleFabricLoaderVersion(request.loaderVersion)
        val profileUrl = fabricLoaderProfileUrl(request.minecraftVersion, loaderVersion)
        return FabricCacheMetadata(
            loaderVersion = loaderVersion,
            loaderVersions = loaderVersions,
            profileUrl = profileUrl,
            profile = metadataFetcher.fetchText(profileUrl),
        )
    }

    private fun resolveHandle(handle: String): Path {
        require(!Path.of(handle).isAbsolute) { "cache handle must be relative" }
        val resolved = root.resolve(handle).normalize()
        require(resolved.startsWith(root)) { "cache handle must stay under the workspace root" }
        return resolved
    }

    private fun writeBytesArtifact(
        artifact: CachePreparedArtifact,
        bytes: ByteArray,
    ) {
        val target = resolveHandle(artifact.handle)
        Files.createDirectories(target.parent)
        Files.write(target, bytes)
    }

    private fun writeTextArtifact(
        artifact: CachePreparedArtifact,
        text: String,
    ) {
        val target = resolveHandle(artifact.handle)
        Files.createDirectories(target.parent)
        Files.writeString(target, text)
    }
}

private data class FabricCacheMetadata(
    val loaderVersion: String,
    val loaderVersions: String,
    val profileUrl: String,
    val profile: String,
)

interface CacheMetadataFetcher {
    suspend fun fetchText(url: String): String

    suspend fun fetchBytes(url: String): ByteArray = fetchText(url).encodeToByteArray()
}

class KtorCacheMetadataFetcher : CacheMetadataFetcher {
    override suspend fun fetchText(url: String): String =
        httpClient().use { http ->
            val response = http.get(url)
            require(response.status.isSuccess()) { "metadata fetch failed for $url: ${response.status.value}" }
            response.bodyAsText()
        }

    override suspend fun fetchBytes(url: String): ByteArray =
        httpClient().use { http ->
            val response = http.get(url)
            require(response.status.isSuccess()) { "artifact fetch failed for $url: ${response.status.value}" }
            response.bodyAsBytes()
        }

    private fun httpClient(): HttpClient =
        HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
                requestTimeoutMillis = 120_000
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

private fun String.clientJarUrl(minecraftVersion: String): String =
    Json
        .parseToJsonElement(this)
        .jsonObject["downloads"]
        ?.jsonObject
        ?.get("client")
        ?.jsonObject
        ?.get("url")
        ?.jsonPrimitive
        ?.content
        ?: error("minecraft version $minecraftVersion is missing client jar url")

private fun String.assetIndexMetadata(minecraftVersion: String): AssetIndexMetadata {
    val assetIndex =
        Json
            .parseToJsonElement(this)
            .jsonObject["assetIndex"]
            ?.jsonObject
            ?: error("minecraft version $minecraftVersion is missing asset index metadata")
    val url = assetIndex["url"]?.jsonPrimitive?.content ?: error("minecraft version $minecraftVersion is missing asset index url")
    return AssetIndexMetadata(url)
}

private data class AssetIndexMetadata(
    val url: String,
)

private fun String.compatibleFabricLoaderVersion(requestedLoaderVersion: String?): String {
    val versions =
        Json
            .parseToJsonElement(this)
            .jsonArray
            .mapNotNull { entry ->
                val loader = entry.jsonObject["loader"]?.jsonObject ?: return@mapNotNull null
                val version = loader["version"]?.jsonPrimitive?.content ?: return@mapNotNull null
                FabricLoaderVersion(
                    version = version,
                    stable = loader["stable"]?.jsonPrimitive?.booleanOrNull == true,
                )
            }
    requestedLoaderVersion?.let { requested ->
        return versions.firstOrNull { it.version == requested }?.version
            ?: error("fabric loader version $requested is not compatible with this minecraft version")
    }
    return versions.firstOrNull { it.stable }?.version
        ?: versions.firstOrNull()?.version
        ?: error("no compatible fabric loader version was found")
}

private data class FabricLoaderVersion(
    val version: String,
    val stable: Boolean,
)

private fun fabricLoaderVersionsUrl(minecraftVersion: String): String = "$FABRIC_META_BASE_URL/versions/loader/$minecraftVersion"

private fun fabricLoaderProfileUrl(
    minecraftVersion: String,
    loaderVersion: String,
): String = "$FABRIC_META_BASE_URL/versions/loader/$minecraftVersion/$loaderVersion/profile/json"

private fun String.fabricLibraries(): List<FabricLibraryArtifact> =
    Json
        .parseToJsonElement(this)
        .jsonObject["libraries"]
        ?.jsonArray
        .orEmpty()
        .mapNotNull { library ->
            val item = library.jsonObject
            item["downloads"]
                ?.jsonObject
                ?.get("artifact")
                ?.jsonObject
                ?.let { artifact ->
                    val url = artifact["url"]?.jsonPrimitive?.content ?: return@let null
                    return@mapNotNull FabricLibraryArtifact(url)
                }
            val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val baseUrl = item["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val path = name.mavenPath()
            FabricLibraryArtifact(baseUrl.trimEnd('/') + "/$path")
        }

private fun String.assetObjects(): List<MinecraftAssetObject> =
    Json
        .parseToJsonElement(this)
        .jsonObject["objects"]
        ?.jsonObject
        .orEmpty()
        .values
        .mapNotNull { asset ->
            val hash = asset.jsonObject["hash"]?.jsonPrimitive?.content ?: return@mapNotNull null
            MinecraftAssetObject(hash)
        }

private data class MinecraftAssetObject(
    val hash: String,
) {
    val source: String = "$MINECRAFT_ASSET_BASE_URL/${hash.take(2)}/$hash"

    val artifact: CachePreparedArtifact =
        CachePreparedArtifact(
            kind = CachePreparedArtifactKind.MINECRAFT_ASSET_OBJECT,
            handle = "cache/assets/objects/${hash.sha256Hex()}.asset",
            source = source,
            status = CachePreparedArtifactStatus.CACHED,
        )
}

private const val MINECRAFT_ASSET_BASE_URL = "https://resources.download.minecraft.net"

private data class FabricLibraryArtifact(
    val source: String,
) {
    private val handle: String = "cache/libraries/fabric/${source.sha256Hex()}.jar"

    val artifact: CachePreparedArtifact =
        CachePreparedArtifact(
            kind = CachePreparedArtifactKind.FABRIC_LIBRARY,
            handle = handle,
            status = CachePreparedArtifactStatus.CACHED,
        )
}

private fun String.sha256Hex(): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

private fun String.mavenPath(): String {
    val parts = split(':')
    require(parts.size >= 3) { "maven coordinate $this must include group, artifact, and version" }
    val group = parts[0].replace('.', '/')
    val artifact = parts[1]
    val version = parts[2]
    val classifier =
        parts
            .getOrNull(3)
            ?.takeUnless { it.isBlank() }
            ?.let { "-$it" }
            .orEmpty()
    return "$group/$artifact/$version/$artifact-$version$classifier.jar"
}
