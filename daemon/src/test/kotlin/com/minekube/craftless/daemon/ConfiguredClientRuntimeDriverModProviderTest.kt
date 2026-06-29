package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.Loader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfiguredClientRuntimeDriverModProviderTest {
    @Test
    fun `manifest provides preferred loader version for matching fabric lane`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest-preferred-loader")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "1.21.6",
                  "loaderVersion": "0.16.14",
                  "path": "mods/craftless-driver-fabric-1.21.6.jar"
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                    ),
            )

        val preferred =
            provider.preferredLoaderVersion(
                ClientRuntimeDriverModRequest(
                    loader = Loader.FABRIC,
                    minecraftVersion = "1.21.6",
                    loaderVersion = null,
                ),
            )

        assertEquals("0.16.14", preferred)
    }

    @Test
    fun `manifest exposes configured driver mod versions`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest-versions")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "26.2",
                  "loaderVersion": "0.19.3",
                  "fabricApiVersion": "0.153.0+26.2",
                  "javaMajorVersion": 25,
                  "mappingsFingerprint": "craftless-fabric-official-26-2",
                  "path": "mods/fabric-26.2/craftless-driver-fabric-official.jar",
                  "runtimeMods": [
                    "mods/fabric-26.2/runtime/pathing.jar"
                  ]
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                    ),
            )

        val versions = provider.driverModVersions()
        val entry = versions.entries.single()

        assertEquals("manifest", versions.source)
        assertEquals(Loader.FABRIC, entry.loader)
        assertEquals("26.2", entry.minecraftVersion)
        assertEquals("0.19.3", entry.loaderVersion)
        assertEquals("0.153.0+26.2", entry.fabricApiVersion)
        assertEquals(25, entry.javaMajorVersion)
        assertEquals("craftless-fabric-official-26-2", entry.mappingsFingerprint)
        assertEquals("mods/fabric-26.2/craftless-driver-fabric-official.jar", entry.path)
        assertEquals(listOf("mods/fabric-26.2/runtime/pathing.jar"), entry.runtimeMods)
    }

    @Test
    fun `manifest exact runtime lane wins over single fabric fallback`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest")
        val manifestMod = root.resolve("mods/craftless-driver-fabric-1.21.6.jar")
        val fallbackMod = root.resolve("mods/craftless-driver-fabric-fallback.jar")
        Files.createDirectories(manifestMod.parent)
        Files.writeString(manifestMod, "manifest-driver")
        Files.writeString(fallbackMod, "fallback-driver")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "1.21.6",
                  "loaderVersion": "0.17.2",
                  "path": "mods/craftless-driver-fabric-1.21.6.jar"
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_FABRIC_DRIVER_MOD to fallbackMod.toString(),
                    ),
            )

        val selected =
            provider.modFor(
                ClientRuntimeDriverModRequest(
                    loader = Loader.FABRIC,
                    minecraftVersion = "1.21.6",
                    loaderVersion = "0.17.2",
                ),
            )

        assertEquals(manifestMod.toAbsolutePath().normalize(), selected?.toAbsolutePath()?.normalize())
    }

    @Test
    fun `manifest resolves private runtime mods for selected fabric lane`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest-runtime-mods")
        val manifestMod = root.resolve("mods/fabric-1.20.6/craftless-driver-fabric.jar")
        val navigationRuntime = root.resolve("mods/fabric-1.20.6/runtime/navigation-runtime.jar")
        val navigationNestedRuntime = root.resolve("mods/fabric-1.20.6/runtime/navigation-nested-runtime.jar")
        Files.createDirectories(navigationRuntime.parent)
        Files.writeString(manifestMod, "manifest-driver")
        Files.writeString(navigationRuntime, "navigation-runtime")
        Files.writeString(navigationNestedRuntime, "navigation-nested-runtime")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "1.20.6",
                  "loaderVersion": "0.19.3",
                  "path": "mods/fabric-1.20.6/craftless-driver-fabric.jar",
                  "runtimeMods": [
                    "mods/fabric-1.20.6/runtime/navigation-runtime.jar",
                    "mods/fabric-1.20.6/runtime/navigation-nested-runtime.jar"
                  ]
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                    ),
            )

        val selected =
            provider.modsFor(
                ClientRuntimeDriverModRequest(
                    loader = Loader.FABRIC,
                    minecraftVersion = "1.20.6",
                    loaderVersion = "0.19.3",
                ),
            )

        assertEquals(manifestMod.toAbsolutePath().normalize(), selected.primary?.toAbsolutePath()?.normalize())
        assertEquals(
            listOf(
                navigationRuntime.toAbsolutePath().normalize(),
                navigationNestedRuntime.toAbsolutePath().normalize(),
            ),
            selected.runtimeMods.map { it.toAbsolutePath().normalize() },
        )
    }

    @Test
    fun `manifest fabric api mismatch rejects runtime identity`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest-fabric-api")
        val manifestMod = root.resolve("mods/craftless-driver-fabric-1.21.6.jar")
        Files.createDirectories(manifestMod.parent)
        Files.writeString(manifestMod, "manifest-driver")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "1.21.6",
                  "loaderVersion": "0.17.2",
                  "fabricApiVersion": "0.127.0+1.21.6",
                  "path": "mods/craftless-driver-fabric-1.21.6.jar"
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                    ),
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                provider.modFor(
                    ClientRuntimeDriverModRequest(
                        loader = Loader.FABRIC,
                        minecraftVersion = "1.21.6",
                        loaderVersion = "0.17.2",
                        fabricApiVersion = "0.128.2+1.21.6",
                    ),
                )
            }

        assertTrue(error.message?.contains("driver mod manifest") == true)
        assertTrue(error.message?.contains("1.21.6") == true)
        assertTrue(error.message?.contains("0.17.2") == true)
        assertTrue(error.message?.contains("0.128.2+1.21.6") == true)
    }

    @Test
    fun `manifest java major mismatch rejects runtime identity`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest-java")
        val manifestMod = root.resolve("mods/craftless-driver-fabric-1.21.6.jar")
        Files.createDirectories(manifestMod.parent)
        Files.writeString(manifestMod, "manifest-driver")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "1.21.6",
                  "loaderVersion": "0.17.2",
                  "javaMajorVersion": 17,
                  "path": "mods/craftless-driver-fabric-1.21.6.jar"
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                    ),
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                provider.modFor(
                    ClientRuntimeDriverModRequest(
                        loader = Loader.FABRIC,
                        minecraftVersion = "1.21.6",
                        loaderVersion = "0.17.2",
                        javaMajorVersion = 21,
                    ),
                )
            }

        assertTrue(error.message?.contains("driver mod manifest") == true)
        assertTrue(error.message?.contains("javaMajorVersion=21") == true)
    }

    @Test
    fun `manifest misses reject single fabric driver mod fallback`() {
        val root = Files.createTempDirectory("craftless-driver-mod-manifest-fallback")
        val manifestMod = root.resolve("mods/craftless-driver-fabric-1.21.6.jar")
        val fallbackMod = root.resolve("mods/craftless-driver-fabric-fallback.jar")
        Files.createDirectories(manifestMod.parent)
        Files.writeString(manifestMod, "manifest-driver")
        Files.writeString(fallbackMod, "fallback-driver")
        val manifest = root.resolve("driver-mods.json")
        Files.writeString(
            manifest,
            """
            {
              "entries": [
                {
                  "loader": "FABRIC",
                  "minecraftVersion": "1.21.6",
                  "loaderVersion": "0.17.2",
                  "path": "mods/craftless-driver-fabric-1.21.6.jar"
                }
              ]
            }
            """.trimIndent(),
        )
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_DRIVER_MOD_MANIFEST to manifest.toString(),
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_FABRIC_DRIVER_MOD to fallbackMod.toString(),
                    ),
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                provider.modFor(
                    ClientRuntimeDriverModRequest(
                        loader = Loader.FABRIC,
                        minecraftVersion = "1.21.7",
                        loaderVersion = "0.17.2",
                    ),
                )
            }

        assertTrue(error.message?.contains("driver mod manifest") == true)
        assertTrue(error.message?.contains("1.21.7") == true)
        assertTrue(error.message?.contains("0.17.2") == true)
    }

    @Test
    fun `single fabric driver mod fallback works when manifest is absent`() {
        val fallbackMod = Files.createTempFile("craftless-driver-fabric-fallback", ".jar")
        Files.writeString(fallbackMod, "fallback-driver")
        val provider =
            ConfiguredClientRuntimeDriverModProvider(
                environment =
                    mapOf(
                        ConfiguredClientRuntimeDriverModProvider.CRAFTLESS_FABRIC_DRIVER_MOD to fallbackMod.toString(),
                    ),
            )

        val selected =
            provider.modFor(
                ClientRuntimeDriverModRequest(
                    loader = Loader.FABRIC,
                    minecraftVersion = "1.21.7",
                    loaderVersion = "0.17.2",
                ),
            )

        assertEquals(fallbackMod.toAbsolutePath().normalize(), selected?.toAbsolutePath()?.normalize())
    }
}
