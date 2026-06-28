package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.driver.api.ConnectionTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal interface OfficialFabricClientConnector {
    fun connect(target: ConnectionTarget): Boolean
}

internal class MinecraftOfficialFabricClientConnector(
    private val clientProvider: () -> Minecraft = Minecraft::getInstance,
) : OfficialFabricClientConnector {
    override fun connect(target: ConnectionTarget): Boolean =
        runCatching {
            connectOnClient(clientProvider(), target)
        }.getOrDefault(false)

    private fun connectOnClient(
        client: Minecraft,
        target: ConnectionTarget,
    ): Boolean {
        if (client.isSameThread) {
            return startConnecting(client, target)
        }
        val result = CompletableFuture<Boolean>()
        client.execute {
            runCatching {
                startConnecting(client, target)
            }.onSuccess(result::complete)
                .onFailure(result::completeExceptionally)
        }
        return result.get(CONNECT_SCHEDULE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    private fun startConnecting(
        client: Minecraft,
        target: ConnectionTarget,
    ): Boolean {
        val hostAndPort = "${target.host}:${target.port}"
        val address = ServerAddress.parseString(hostAndPort)
        val serverData = ServerData("Craftless $hostAndPort", hostAndPort, ServerData.Type.OTHER)
        ConnectScreen.startConnecting(
            TitleScreen(),
            client,
            address,
            serverData,
            false,
            null,
        )
        return true
    }

    private companion object {
        const val CONNECT_SCHEDULE_TIMEOUT_MS = 2_000L
    }
}
