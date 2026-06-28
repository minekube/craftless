package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import net.fabricmc.loader.api.FabricLoader
import java.security.MessageDigest

internal fun interface OfficialFabricRuntimeMetadataProvider {
    fun runtimeMetadata(clientId: String): DriverRuntimeMetadata
}

internal data class OfficialFabricInstalledMod(
    val id: String,
    val version: String,
)

internal data class OfficialFabricRuntimeMetadataSnapshot(
    val loaderVersion: String,
    val installedMods: List<OfficialFabricInstalledMod>,
) {
    init {
        require(loaderVersion.isNotBlank()) { "loader version is required" }
        require(installedMods.isNotEmpty()) { "installed mods are required" }
        installedMods.forEach { mod ->
            require(mod.id.isNotBlank()) { "installed mod id is required" }
            require(mod.version.isNotBlank()) { "installed mod version is required" }
        }
    }
}

internal class SnapshotOfficialFabricRuntimeMetadataProvider(
    private val snapshot: OfficialFabricRuntimeMetadataSnapshot,
) : OfficialFabricRuntimeMetadataProvider {
    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata =
        DriverRuntimeMetadata(
            loaderVersion = snapshot.loaderVersion,
            driver = "craftless-driver-fabric-official",
            driverVersion = "0.1.0-SNAPSHOT",
            mappings = "craftless-official-bindings-26-2",
            installedModsFingerprint = fingerprint("mods", snapshot.installedMods.map { mod -> "${mod.id}@${mod.version}" }),
            registryFingerprint = "registries:not-discovered",
            serverFeatureFingerprint = "server-features:not-connected",
            permissionsFingerprint = "permissions:local-client",
        )
}

internal class FabricLoaderOfficialRuntimeMetadataProvider(
    private val loader: FabricLoader = FabricLoader.getInstance(),
) : OfficialFabricRuntimeMetadataProvider {
    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata =
        SnapshotOfficialFabricRuntimeMetadataProvider(loaderSnapshot()).runtimeMetadata(clientId)

    private fun loaderSnapshot(): OfficialFabricRuntimeMetadataSnapshot =
        OfficialFabricRuntimeMetadataSnapshot(
            loaderVersion =
                loader
                    .getModContainer("fabricloader")
                    .map { container -> container.metadata.version.friendlyString }
                    .orElse("unknown"),
            installedMods =
                loader.allMods.map { container ->
                    OfficialFabricInstalledMod(
                        id = container.metadata.id,
                        version = container.metadata.version.friendlyString,
                    )
                },
        )
}

private fun fingerprint(
    prefix: String,
    values: List<String>,
): String {
    val canonical = values.sorted().joinToString(separator = "\n")
    val digest =
        MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.encodeToByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(16)
    return "$prefix:$digest"
}
