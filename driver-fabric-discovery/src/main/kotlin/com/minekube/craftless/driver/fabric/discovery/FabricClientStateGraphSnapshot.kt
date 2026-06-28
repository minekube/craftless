package com.minekube.craftless.driver.fabric.discovery

import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeHandleNode
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSchema

data class FabricClientStateGraphSnapshot(
    val connected: Boolean,
    val player: Boolean,
    val inventory: Boolean,
    val camera: Boolean,
    val interactionManager: Boolean,
    val world: Boolean,
    val recipes: Boolean = false,
    val recipeCrafting: Boolean = false,
) {
    companion object {
        fun disconnected(): FabricClientStateGraphSnapshot =
            FabricClientStateGraphSnapshot(
                connected = false,
                player = false,
                inventory = false,
                camera = false,
                interactionManager = false,
                world = false,
            )
    }
}

fun fabricClientStateGraphFragment(snapshot: FabricClientStateGraphSnapshot): FabricRuntimeGraphFragment {
    val inventoryAvailability = snapshot.inventoryAvailability()
    val recipeQueryAvailability = snapshot.recipeQueryAvailability()
    val worldAvailability = snapshot.worldAvailability()
    val entityAvailability = snapshot.entityAvailability()

    return FabricRuntimeGraphFragment(
        resources =
            listOf(
                RuntimeResourceNode("client", snapshot.connectedAvailability()),
                RuntimeResourceNode("player", snapshot.playerAvailability()),
                RuntimeResourceNode("inventory", inventoryAvailability),
                RuntimeResourceNode("recipe", recipeQueryAvailability),
                RuntimeResourceNode("world", worldAvailability),
                RuntimeResourceNode("entity", entityAvailability),
                RuntimeResourceNode("screen", RuntimeAvailability.available()),
            ),
        handles =
            listOf(
                RuntimeHandleNode(
                    id = "inventory.slot",
                    resource = "inventory",
                    schema = RuntimeSchema.objectSchema(),
                    availability = inventoryAvailability,
                ),
                RuntimeHandleNode(
                    id = "recipe.handle",
                    resource = "recipe",
                    schema = RuntimeSchema.objectSchema(),
                    availability = recipeQueryAvailability,
                ),
                RuntimeHandleNode(
                    id = "world.block.handle",
                    resource = "world",
                    schema = RuntimeSchema.objectSchema(),
                    availability = snapshot.blockQueryAvailability(),
                ),
                RuntimeHandleNode(
                    id = "entity.handle",
                    resource = "entity",
                    schema = RuntimeSchema.objectSchema(),
                    availability = entityAvailability,
                ),
            ),
    )
}

private fun FabricClientStateGraphSnapshot.connectedAvailability(): RuntimeAvailability =
    if (connected) RuntimeAvailability.available() else RuntimeAvailability.unavailable("client-not-connected")

private fun FabricClientStateGraphSnapshot.playerAvailability(): RuntimeAvailability = availability(playerReason())

private fun FabricClientStateGraphSnapshot.inventoryAvailability(): RuntimeAvailability = availability(inventoryReason())

private fun FabricClientStateGraphSnapshot.recipeQueryAvailability(): RuntimeAvailability = availability(recipeQueryReason())

private fun FabricClientStateGraphSnapshot.worldAvailability(): RuntimeAvailability = availability(worldReason())

private fun FabricClientStateGraphSnapshot.entityAvailability(): RuntimeAvailability = availability(entityReason())

private fun FabricClientStateGraphSnapshot.blockQueryAvailability(): RuntimeAvailability = availability(blockQueryReason())

private fun FabricClientStateGraphSnapshot.playerReason(): String? =
    when {
        !connected -> "client-not-connected"
        !player -> "player-unavailable"
        else -> null
    }

private fun FabricClientStateGraphSnapshot.inventoryReason(): String? =
    when {
        !connected -> "client-not-connected"
        !inventory -> "inventory-unavailable"
        else -> null
    }

private fun FabricClientStateGraphSnapshot.recipeQueryReason(): String? =
    playerReason()
        ?: inventoryReason()
        ?: if (!recipes) "recipe-discovery-unavailable" else null

private fun FabricClientStateGraphSnapshot.worldReason(): String? =
    when {
        !connected -> "client-not-connected"
        !world -> "world-unavailable"
        else -> null
    }

private fun FabricClientStateGraphSnapshot.entityReason(): String? =
    playerReason()
        ?: worldReason()

private fun FabricClientStateGraphSnapshot.blockQueryReason(): String? =
    playerReason()
        ?: worldReason()

private fun availability(reason: String?): RuntimeAvailability =
    if (reason == null) RuntimeAvailability.available() else RuntimeAvailability.unavailable(reason)
