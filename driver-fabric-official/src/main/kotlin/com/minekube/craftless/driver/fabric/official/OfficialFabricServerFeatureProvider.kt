package com.minekube.craftless.driver.fabric.official

import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal fun interface OfficialFabricServerFeatureProvider {
    fun serverFeatures(): List<String>
}

internal class MinecraftOfficialFabricServerFeatureProvider(
    private val clientProvider: () -> Minecraft = Minecraft::getInstance,
) : OfficialFabricServerFeatureProvider {
    override fun serverFeatures(): List<String> =
        try {
            queryOnClient(clientProvider()) {
                val connection = getConnection()
                listOf(
                    "connection:${if (connection != null && player != null) "connected" else "disconnected"}",
                    "server:${serverKind()}",
                    "local-server:$isLocalServer",
                    "feature-set:${connection?.enabledFeatures()?.hashCode() ?: "none"}",
                )
            }
        } catch (_: RuntimeException) {
            listOf("connection:unknown", "server:unknown", "feature-set:unavailable")
        }

    private fun <T> queryOnClient(
        client: Minecraft,
        query: Minecraft.() -> T,
    ): T {
        if (client.isSameThread) {
            return client.query()
        }
        val result = CompletableFuture<T>()
        client.execute {
            runCatching {
                client.query()
            }.onSuccess(result::complete)
                .onFailure(result::completeExceptionally)
        }
        return result.get(CLIENT_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    private fun Minecraft.serverKind(): String =
        when {
            hasSingleplayerServer() -> "singleplayer"
            getCurrentServer()?.isLan == true -> "local"
            getCurrentServer()?.isRealm == true -> "realm"
            getCurrentServer() != null -> "remote"
            else -> "none"
        }

    private companion object {
        const val CLIENT_QUERY_TIMEOUT_MS = 2_000L
    }
}
