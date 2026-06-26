package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.RuntimeSourceEvidence
import java.util.concurrent.atomic.AtomicLong

object FabricEventHooks {
    private const val CLIENT_TICK_SOURCE = "craftless-client-tick"

    private val clientTicks = AtomicLong()

    @JvmStatic
    fun recordClientTick() {
        clientTicks.incrementAndGet()
    }

    @JvmStatic
    fun snapshot(): FabricEventHookSnapshot = FabricEventHookSnapshot(clientTicks = clientTicks.get())

    @JvmStatic
    fun sourceEvidence(): List<RuntimeSourceEvidence> = listOf(RuntimeSourceEvidence("mixin", CLIENT_TICK_SOURCE))
}

data class FabricEventHookSnapshot(
    val clientTicks: Long,
)
