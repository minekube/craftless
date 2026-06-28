package com.minekube.craftless.testkit

import com.minekube.craftless.protocol.MINECRAFT_VERSION_INDEX_URL
import com.minekube.craftless.protocol.resolveMinecraftVersion
import com.minekube.craftless.protocol.versionManifestUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class MinecraftServerJarProvisioner(
    private val http: HttpClient,
    private val manifestUrl: String = MINECRAFT_VERSION_INDEX_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun provision(
        version: String,
        artifactsDir: Path,
    ): Path {
        require(version.isNotBlank()) { "minecraft server version is required" }
        val manifest = http.get(manifestUrl).bodyAsText()
        val resolvedVersion = manifest.resolveMinecraftVersion(version)
        val versionUrl = manifest.versionManifestUrl(resolvedVersion)
        val versionMetadata =
            json.decodeFromString<LauncherVersionMetadata>(
                http.get(versionUrl).bodyAsText(),
            )
        val serverUrl =
            versionMetadata.downloads.server?.url
                ?: error("minecraft server version $resolvedVersion does not include a server download")
        val target = artifactsDir.resolve("minecraft-server-$resolvedVersion.jar")
        Files.createDirectories(target.parent)
        Files.write(target, http.get(serverUrl).body<ByteArray>())
        return target
    }
}

suspend fun LocalServerLayout.provisionMinecraftServerJar(
    version: String,
    provisioner: MinecraftServerJarProvisioner,
): Path =
    provisioner.provision(
        version = version,
        artifactsDir = artifactsDir,
    )

@Serializable
private data class LauncherVersionMetadata(
    val downloads: LauncherDownloads = LauncherDownloads(),
)

@Serializable
private data class LauncherDownloads(
    val server: LauncherDownload? = null,
)

@Serializable
private data class LauncherDownload(
    val url: String,
)
