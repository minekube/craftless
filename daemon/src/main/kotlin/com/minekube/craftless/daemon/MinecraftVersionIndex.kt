package com.minekube.craftless.daemon

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun String.versionManifestUrl(minecraftVersion: String): String {
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

internal fun String.resolveMinecraftVersion(minecraftVersion: String): String {
    val latestField =
        when (minecraftVersion) {
            "latest-release" -> "release"
            "latest-snapshot" -> "snapshot"
            else -> return minecraftVersion
        }
    val latest =
        Json
            .parseToJsonElement(this)
            .jsonObject["latest"]
            ?.jsonObject
            ?: error("minecraft version index is missing latest aliases")
    val resolved =
        latest[latestField]
            ?.jsonPrimitive
            ?.content
            ?: error("minecraft version index is missing latest.$latestField")
    requireFileSafeCacheSegment(resolved, "resolved Minecraft version")
    return resolved
}

internal fun requireFileSafeCacheSegment(
    value: String,
    label: String,
) {
    require(value.isNotBlank()) { "$label is required" }
    require(!value.contains('/')) { "$label must be a file-safe segment" }
    require(!value.contains('\\')) { "$label must use forward slashes" }
    require(!value.contains("..")) { "$label must be a file-safe segment" }
}
