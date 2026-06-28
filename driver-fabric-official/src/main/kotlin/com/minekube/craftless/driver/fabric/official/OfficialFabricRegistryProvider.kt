package com.minekube.craftless.driver.fabric.official

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries

internal fun interface OfficialFabricRegistryProvider {
    fun registryEntries(): List<String>
}

internal class MinecraftOfficialFabricRegistryProvider : OfficialFabricRegistryProvider {
    override fun registryEntries(): List<String> =
        try {
            listOf(
                registryEntries("block", BuiltInRegistries.BLOCK),
                registryEntries("item", BuiltInRegistries.ITEM),
                registryEntries("entity-type", BuiltInRegistries.ENTITY_TYPE),
                registryEntries("screen-handler", BuiltInRegistries.MENU),
                registryEntries("status-effect", BuiltInRegistries.MOB_EFFECT),
                registryEntries("game-event", BuiltInRegistries.GAME_EVENT),
            ).flatten()
        } catch (_: IllegalArgumentException) {
            listOf(UNAVAILABLE_REGISTRY_ENTRY)
        } catch (_: IllegalStateException) {
            listOf(UNAVAILABLE_REGISTRY_ENTRY)
        } catch (_: ExceptionInInitializerError) {
            listOf(UNAVAILABLE_REGISTRY_ENTRY)
        } catch (_: NoClassDefFoundError) {
            listOf(UNAVAILABLE_REGISTRY_ENTRY)
        }

    private fun registryEntries(
        label: String,
        registry: Registry<*>,
    ): List<String> = registry.keySet().map { id -> "$label:$id" }

    private companion object {
        const val UNAVAILABLE_REGISTRY_ENTRY = "registry:unavailable-unbootstrapped"
    }
}
