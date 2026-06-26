package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.CachePrepareRequest
import com.minekube.craftless.protocol.CachePreparedArtifactKind
import com.minekube.craftless.protocol.FABRIC_META_BASE_URL
import com.minekube.craftless.protocol.Loader
import com.minekube.craftless.protocol.MINECRAFT_VERSION_INDEX_URL
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CachePreparationServiceTest {
    @Test
    fun `cache preparation resolves and stores minecraft version metadata`() =
        runBlocking {
            val workspace = Files.createTempDirectory("craftless-cache-resolution")
            val versionUrl = "https://metadata.test/1.21.6.json"
            val clientJarUrl = "https://metadata.test/client.jar"
            val assetIndexUrl = "https://metadata.test/assets/1.21.6.json"
            val loaderVersionsUrl = "$FABRIC_META_BASE_URL/versions/loader/1.21.6"
            val loaderProfileUrl = "$FABRIC_META_BASE_URL/versions/loader/1.21.6/0.17.2/profile/json"
            val fabricLoaderJarUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.17.2/fabric-loader-0.17.2.jar"
            val service =
                CachePreparationService(
                    workspaceRoot = workspace,
                    metadataFetcher =
                        StaticCacheMetadataFetcher(
                            mapOf(
                                MINECRAFT_VERSION_INDEX_URL to
                                    """
                                    {
                                      "versions": [
                                        { "id": "1.21.6", "url": "$versionUrl" }
                                      ]
                                    }
                                    """.trimIndent(),
                                versionUrl to
                                    """
                                    {
                                      "id": "1.21.6",
                                      "assetIndex": {
                                        "id": "1.21.6",
                                        "url": "$assetIndexUrl"
                                      },
                                      "downloads": {
                                        "client": { "url": "$clientJarUrl" }
                                      }
                                    }
                                    """.trimIndent(),
                                assetIndexUrl to
                                    """
                                    {
                                      "objects": {
                                        "minecraft/sounds/random/test.ogg": {
                                          "hash": "abcdef0123456789abcdef0123456789abcdef01",
                                          "size": 10
                                        }
                                      }
                                    }
                                    """.trimIndent(),
                                loaderVersionsUrl to
                                    """
                                    [
                                      { "loader": { "version": "0.17.1", "stable": false } },
                                      { "loader": { "version": "0.17.2", "stable": true } }
                                    ]
                                    """.trimIndent(),
                                loaderProfileUrl to
                                    """
                                    {
                                      "id": "fabric-loader-0.17.2-1.21.6",
                                      "libraries": [
                                        {
                                          "name": "net.fabricmc:fabric-loader:0.17.2",
                                          "url": "https://maven.fabricmc.net/"
                                        }
                                      ]
                                    }
                                    """.trimIndent(),
                            ),
                            binaryResponses =
                                mapOf(
                                    clientJarUrl to "client-jar".encodeToByteArray(),
                                    fabricLoaderJarUrl to "fabric-loader-jar".encodeToByteArray(),
                                ),
                        ),
                )

            val result = service.prepare(CachePrepareRequest("1.21.6", Loader.FABRIC))

            assertEquals(
                listOf(
                    CachePreparedArtifactKind.MINECRAFT_VERSION_INDEX,
                    CachePreparedArtifactKind.MINECRAFT_VERSION_MANIFEST,
                    CachePreparedArtifactKind.MINECRAFT_CLIENT_JAR,
                    CachePreparedArtifactKind.MINECRAFT_ASSET_INDEX,
                    CachePreparedArtifactKind.FABRIC_LOADER_VERSIONS,
                    CachePreparedArtifactKind.FABRIC_LOADER_PROFILE,
                    CachePreparedArtifactKind.MINECRAFT_ASSET_OBJECT,
                    CachePreparedArtifactKind.FABRIC_LIBRARY,
                ),
                result.artifacts.map { it.kind },
            )
            assertEquals("0.17.2", result.loaderVersion)
            assertEquals(versionUrl, result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_VERSION_MANIFEST }.source)
            assertEquals(clientJarUrl, result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_CLIENT_JAR }.source)
            assertEquals(assetIndexUrl, result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_ASSET_INDEX }.source)
            assertEquals(loaderProfileUrl, result.artifacts.single { it.kind == CachePreparedArtifactKind.FABRIC_LOADER_PROFILE }.source)
            val assetObject = result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_ASSET_OBJECT }
            assertEquals(null, assetObject.source)
            assertTrue(assetObject.handle.startsWith("cache/assets/objects/"))
            assertEquals("INDEXED", assetObject.status.name)
            val fabricLibrary = result.artifacts.single { it.kind == CachePreparedArtifactKind.FABRIC_LIBRARY }
            assertEquals(null, fabricLibrary.source)
            assertTrue(fabricLibrary.handle.startsWith("cache/libraries/fabric/"))
            assertEquals(
                listOf(
                    fabricLibrary.handle,
                    "cache/minecraft/versions/1.21.6/client.jar",
                ),
                result.launch.classpath,
            )
            assertTrue(Files.readString(workspace.resolve("cache/minecraft/version_manifest_v2.json")).contains("1.21.6"))
            assertTrue(Files.readString(workspace.resolve("cache/minecraft/versions/1.21.6/version.json")).contains("client.jar"))
            assertEquals("client-jar", Files.readString(workspace.resolve("cache/minecraft/versions/1.21.6/client.jar")))
            assertTrue(Files.readString(workspace.resolve("cache/assets/indexes/1.21.6.json")).contains("test.ogg"))
            assertTrue(!Files.exists(workspace.resolve(assetObject.handle)))
            assertTrue(Files.readString(workspace.resolve("cache/loaders/fabric/1.21.6/versions.json")).contains("0.17.2"))
            assertTrue(Files.readString(workspace.resolve("cache/loaders/fabric/1.21.6/0.17.2/profile.json")).contains("fabric-loader"))
            assertEquals(
                "fabric-loader-jar",
                Files.readString(workspace.resolve(fabricLibrary.handle)),
            )
            assertTrue(Files.readString(workspace.resolve(result.manifest)).contains("MINECRAFT_VERSION_MANIFEST"))
            assertTrue(Files.readString(workspace.resolve(result.manifest)).contains("FABRIC_LIBRARY"))
        }

    @Test
    fun `cache preparation uses pinned compatible fabric loader version`() =
        runBlocking {
            val workspace = Files.createTempDirectory("craftless-cache-loader-pin")
            val versionUrl = "https://metadata.test/1.21.6.json"
            val clientJarUrl = "https://metadata.test/client.jar"
            val assetIndexUrl = "https://metadata.test/assets/1.21.6.json"
            val loaderVersionsUrl = "$FABRIC_META_BASE_URL/versions/loader/1.21.6"
            val pinnedProfileUrl = "$FABRIC_META_BASE_URL/versions/loader/1.21.6/0.16.14/profile/json"
            val service =
                CachePreparationService(
                    workspaceRoot = workspace,
                    metadataFetcher =
                        StaticCacheMetadataFetcher(
                            mapOf(
                                MINECRAFT_VERSION_INDEX_URL to
                                    """
                                    { "versions": [{ "id": "1.21.6", "url": "$versionUrl" }] }
                                    """.trimIndent(),
                                versionUrl to
                                    """
                                    {
                                      "id": "1.21.6",
                                      "assetIndex": {
                                        "id": "1.21.6",
                                        "url": "$assetIndexUrl"
                                      },
                                      "downloads": {
                                        "client": { "url": "$clientJarUrl" }
                                      }
                                    }
                                    """.trimIndent(),
                                assetIndexUrl to """{"objects":{}}""",
                                loaderVersionsUrl to
                                    """
                                    [
                                      { "loader": { "version": "0.17.2", "stable": true } },
                                      { "loader": { "version": "0.16.14", "stable": true } }
                                    ]
                                    """.trimIndent(),
                                pinnedProfileUrl to """{"id":"fabric-loader-0.16.14-1.21.6"}""",
                            ),
                            binaryResponses = mapOf(clientJarUrl to "client-jar".encodeToByteArray()),
                        ),
                )

            val result =
                service.prepare(
                    CachePrepareRequest(
                        minecraftVersion = "1.21.6",
                        loader = Loader.FABRIC,
                        loaderVersion = "0.16.14",
                    ),
                )

            assertEquals("0.16.14", result.loaderVersion)
            assertEquals("cache/loaders/fabric/1.21.6/0.16.14", result.loaderRoot)
            assertTrue(Files.readString(workspace.resolve(result.manifest)).contains("0.16.14"))
            assertTrue(Files.readString(workspace.resolve("cache/loaders/fabric/1.21.6/0.16.14/profile.json")).contains("0.16.14"))
        }
}

private class StaticCacheMetadataFetcher(
    private val responses: Map<String, String>,
    private val binaryResponses: Map<String, ByteArray> = emptyMap(),
) : CacheMetadataFetcher {
    override suspend fun fetchText(url: String): String = requireNotNull(responses[url]) { "missing test response for $url" }

    override suspend fun fetchBytes(url: String): ByteArray =
        requireNotNull(binaryResponses[url]) {
            "missing test binary response for $url"
        }
}
