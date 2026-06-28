package com.minekube.craftless.daemon

import com.minekube.craftless.protocol.Loader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfiguredClientRuntimeDriverModProviderTest {
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
