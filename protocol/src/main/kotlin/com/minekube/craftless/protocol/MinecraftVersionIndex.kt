package com.minekube.craftless.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun String.versionManifestUrl(minecraftVersion: String): String {
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

fun String.resolveMinecraftVersion(minecraftVersion: String): String {
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

fun String.minecraftVersionList(): MinecraftVersionListResult {
    val root = Json.parseToJsonElement(this).jsonObject
    val latest = root["latest"]?.jsonObject ?: error("minecraft version index is missing latest aliases")
    val release = latest["release"]?.jsonPrimitive?.content ?: error("minecraft version index is missing latest.release")
    val snapshot = latest["snapshot"]?.jsonPrimitive?.content ?: error("minecraft version index is missing latest.snapshot")
    val versions =
        root["versions"]
            ?.jsonArray
            .orEmpty()
            .map { version ->
                val item = version.jsonObject
                MinecraftVersionDescriptor(
                    id = item["id"]?.jsonPrimitive?.content ?: error("minecraft version entry is missing id"),
                    type = item["type"]?.jsonPrimitive?.content ?: "unknown",
                    url = item["url"]?.jsonPrimitive?.content,
                )
            }
    return MinecraftVersionListResult(
        latest = MinecraftLatestVersions(release = release, snapshot = snapshot),
        versions = versions,
    )
}

fun requireFileSafeCacheSegment(
    value: String,
    label: String,
) {
    require(value.isNotBlank()) { "$label is required" }
    require(!value.contains('/')) { "$label must be a file-safe segment" }
    require(!value.contains('\\')) { "$label must use forward slashes" }
    require(!value.contains("..")) { "$label must be a file-safe segment" }
}
