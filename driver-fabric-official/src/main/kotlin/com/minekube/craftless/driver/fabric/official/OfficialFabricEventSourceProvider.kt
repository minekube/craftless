package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.protocol.RuntimeSourceEvidence
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal fun interface OfficialFabricEventSourceProvider {
    fun sourceEvidence(): List<RuntimeSourceEvidence>
}

internal object MinecraftOfficialFabricEventSources : OfficialFabricEventSourceProvider {
    private const val CLIENT_TICK_START = "craftless-official-callback-client-tick-start"
    private const val CLIENT_TICK_END = "craftless-official-callback-client-tick-end"
    private const val PLAY_JOIN = "craftless-official-callback-play-join"
    private const val PLAY_DISCONNECT = "craftless-official-callback-play-disconnect"

    private val callbackSources =
        listOf(
            CLIENT_TICK_START,
            CLIENT_TICK_END,
            PLAY_JOIN,
            PLAY_DISCONNECT,
        )

    private val registered = AtomicBoolean()
    private val counters =
        callbackSources.associateWithTo(ConcurrentHashMap()) {
            AtomicLong()
        }

    fun register() {
        if (!registered.compareAndSet(false, true)) {
            return
        }

        ClientTickEvents.START_CLIENT_TICK.register { record(CLIENT_TICK_START) }
        ClientTickEvents.END_CLIENT_TICK.register { record(CLIENT_TICK_END) }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> record(PLAY_JOIN) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> record(PLAY_DISCONNECT) }
    }

    override fun sourceEvidence(): List<RuntimeSourceEvidence> =
        listOf(RuntimeSourceEvidence("event-source", "driver:official")) +
            callbackSources.map { source -> RuntimeSourceEvidence("callback", source) }

    private fun record(source: String) {
        counters.getValue(source).incrementAndGet()
    }
}
