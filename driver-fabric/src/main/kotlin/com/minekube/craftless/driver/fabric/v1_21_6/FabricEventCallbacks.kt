package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.RuntimeSourceEvidence
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal object FabricEventCallbacks {
    private const val CLIENT_TICK_START = "craftless-callback-client-tick-start"
    private const val CLIENT_TICK_END = "craftless-callback-client-tick-end"
    private const val CLIENT_WORLD_CHANGE = "craftless-callback-client-world-change"
    private const val CLIENT_ENTITY_LOAD = "craftless-callback-client-entity-load"
    private const val CLIENT_ENTITY_UNLOAD = "craftless-callback-client-entity-unload"
    private const val PLAY_JOIN = "craftless-callback-play-join"
    private const val PLAY_DISCONNECT = "craftless-callback-play-disconnect"

    private val callbackSources =
        listOf(
            CLIENT_TICK_START,
            CLIENT_TICK_END,
            CLIENT_WORLD_CHANGE,
            CLIENT_ENTITY_LOAD,
            CLIENT_ENTITY_UNLOAD,
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
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ -> record(CLIENT_WORLD_CHANGE) }
        ClientEntityEvents.ENTITY_LOAD.register { _, _ -> record(CLIENT_ENTITY_LOAD) }
        ClientEntityEvents.ENTITY_UNLOAD.register { _, _ -> record(CLIENT_ENTITY_UNLOAD) }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> record(PLAY_JOIN) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> record(PLAY_DISCONNECT) }
    }

    fun snapshot(): FabricEventCallbackSnapshot =
        FabricEventCallbackSnapshot(
            counts = counters.mapValues { (_, counter) -> counter.get() },
        )

    fun sourceEvidence(): List<RuntimeSourceEvidence> = callbackSources.map { source -> RuntimeSourceEvidence("callback", source) }

    private fun record(source: String) {
        counters.getValue(source).incrementAndGet()
    }
}

internal data class FabricEventCallbackSnapshot(
    val counts: Map<String, Long>,
)
