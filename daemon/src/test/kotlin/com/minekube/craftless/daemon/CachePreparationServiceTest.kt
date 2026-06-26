package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.CachePrepareRequest
import com.minekube.craftless.protocol.CachePreparedArtifactKind
import com.minekube.craftless.protocol.FABRIC_META_BASE_URL
import com.minekube.craftless.protocol.Loader
import com.minekube.craftless.protocol.MINECRAFT_VERSION_INDEX_URL
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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
            val minecraftLibraryUrl = "https://libraries.minecraft.net/com/mojang/authlib/6.0.54/authlib-6.0.54.jar"
            val nativeLibraryUrl = "https://libraries.minecraft.net/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives.jar"
            val assetIndexUrl = "https://metadata.test/assets/1.21.6.json"
            val assetObjectUrl = "https://resources.download.minecraft.net/ab/abcdef0123456789abcdef0123456789abcdef01"
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
                                      "libraries": [
                                        {
                                          "name": "com.mojang:authlib:6.0.54",
                                          "downloads": {
                                            "artifact": {
                                              "url": "$minecraftLibraryUrl"
                                            }
                                          }
                                        },
                                        {
                                          "name": "org.lwjgl:lwjgl:3.3.3",
                                          "natives": {
                                            "linux": "natives-linux",
                                            "osx": "natives-osx",
                                            "windows": "natives-windows"
                                          },
                                          "downloads": {
                                            "classifiers": {
                                              "natives-linux": {
                                                "url": "$nativeLibraryUrl"
                                              },
                                              "natives-osx": {
                                                "url": "$nativeLibraryUrl"
                                              },
                                              "natives-windows": {
                                                "url": "$nativeLibraryUrl"
                                              }
                                            }
                                          }
                                        }
                                      ],
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
                                    minecraftLibraryUrl to "minecraft-library".encodeToByteArray(),
                                    nativeLibraryUrl to nativeZipBytes(),
                                    assetObjectUrl to "asset-bytes".encodeToByteArray(),
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
                    CachePreparedArtifactKind.MINECRAFT_LIBRARY,
                    CachePreparedArtifactKind.MINECRAFT_NATIVE_LIBRARY,
                    CachePreparedArtifactKind.MINECRAFT_NATIVE_DIRECTORY,
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
            assertEquals(assetObjectUrl, assetObject.source)
            assertTrue(assetObject.handle.startsWith("cache/assets/objects/"))
            assertEquals("CACHED", assetObject.status.name)
            val minecraftLibrary = result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_LIBRARY }
            assertEquals(minecraftLibraryUrl, minecraftLibrary.source)
            assertTrue(minecraftLibrary.handle.startsWith("cache/libraries/minecraft/"))
            assertEquals("CACHED", minecraftLibrary.status.name)
            val nativeLibrary = result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_NATIVE_LIBRARY }
            assertEquals(nativeLibraryUrl, nativeLibrary.source)
            assertTrue(nativeLibrary.handle.startsWith("cache/libraries/native/"))
            assertEquals("CACHED", nativeLibrary.status.name)
            val nativeDirectory = result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_NATIVE_DIRECTORY }
            assertEquals(null, nativeDirectory.source)
            assertTrue(nativeDirectory.handle.startsWith("cache/natives/"))
            assertEquals("EXTRACTED", nativeDirectory.status.name)
            val fabricLibrary = result.artifacts.single { it.kind == CachePreparedArtifactKind.FABRIC_LIBRARY }
            assertEquals(null, fabricLibrary.source)
            assertTrue(fabricLibrary.handle.startsWith("cache/libraries/fabric/"))
            assertEquals(
                listOf(
                    minecraftLibrary.handle,
                    fabricLibrary.handle,
                    "cache/minecraft/versions/1.21.6/client.jar",
                ),
                result.launch.classpath,
            )
            assertEquals(listOf(nativeDirectory.handle), result.launch.nativePath)
            assertTrue(Files.readString(workspace.resolve("cache/minecraft/version_manifest_v2.json")).contains("1.21.6"))
            assertTrue(Files.readString(workspace.resolve("cache/minecraft/versions/1.21.6/version.json")).contains("client.jar"))
            assertEquals("client-jar", Files.readString(workspace.resolve("cache/minecraft/versions/1.21.6/client.jar")))
            assertEquals("minecraft-library", Files.readString(workspace.resolve(minecraftLibrary.handle)))
            assertTrue(Files.isRegularFile(workspace.resolve(nativeLibrary.handle)))
            assertEquals("native-bytes", Files.readString(workspace.resolve(nativeDirectory.handle).resolve("libcraftless-test.dylib")))
            assertTrue(!Files.exists(workspace.resolve(nativeDirectory.handle).resolve("META-INF")))
            assertTrue(Files.readString(workspace.resolve("cache/assets/indexes/1.21.6.json")).contains("test.ogg"))
            assertEquals("asset-bytes", Files.readString(workspace.resolve(assetObject.handle)))
            assertTrue(Files.readString(workspace.resolve("cache/loaders/fabric/1.21.6/versions.json")).contains("0.17.2"))
            assertTrue(Files.readString(workspace.resolve("cache/loaders/fabric/1.21.6/0.17.2/profile.json")).contains("fabric-loader"))
            assertEquals(
                "fabric-loader-jar",
                Files.readString(workspace.resolve(fabricLibrary.handle)),
            )
            assertTrue(Files.readString(workspace.resolve(result.manifest)).contains("MINECRAFT_VERSION_MANIFEST"))
            assertTrue(Files.readString(workspace.resolve(result.manifest)).contains("MINECRAFT_LIBRARY"))
            assertTrue(Files.readString(workspace.resolve(result.manifest)).contains("MINECRAFT_NATIVE_DIRECTORY"))
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

private fun nativeZipBytes(): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry("libcraftless-test.dylib"))
        zip.write("native-bytes".encodeToByteArray())
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("META-INF/ignored.txt"))
        zip.write("ignored".encodeToByteArray())
        zip.closeEntry()
    }
    return output.toByteArray()
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
