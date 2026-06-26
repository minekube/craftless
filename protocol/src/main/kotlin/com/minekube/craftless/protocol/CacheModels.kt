package com.minekube.craftless.protocol

import kotlinx.serialization.Serializable

@Serializable
data class CachePrepareRequest(
    val minecraftVersion: String,
    val loader: Loader,
) {
    init {
        require(minecraftVersion.isNotBlank()) { "minecraft version is required" }
        require(!minecraftVersion.contains('/')) { "minecraft version must be a file-safe segment" }
        require(!minecraftVersion.contains('\\')) { "minecraft version must use forward slashes" }
        require(!minecraftVersion.contains("..")) { "minecraft version must be a file-safe segment" }
    }
}

@Serializable
data class CachePrepareResult(
    val minecraftVersion: String,
    val loader: Loader,
    val cacheRoot: String,
    val minecraftVersionRoot: String,
    val loaderRoot: String,
    val runtimeRoot: String,
    val manifest: String,
    val status: CachePrepareStatus,
) {
    companion object {
        fun forRequest(request: CachePrepareRequest): CachePrepareResult {
            val loaderPath = request.loader.name.lowercase()
            return CachePrepareResult(
                minecraftVersion = request.minecraftVersion,
                loader = request.loader,
                cacheRoot = "cache",
                minecraftVersionRoot = "cache/minecraft/versions/${request.minecraftVersion}",
                loaderRoot = "cache/loaders/$loaderPath/${request.minecraftVersion}",
                runtimeRoot = "cache/runtimes",
                manifest = "cache/prepared/${request.minecraftVersion}-$loaderPath.json",
                status = CachePrepareStatus.PREPARED,
            )
        }
    }
}

@Serializable
enum class CachePrepareStatus {
    PREPARED,
}
