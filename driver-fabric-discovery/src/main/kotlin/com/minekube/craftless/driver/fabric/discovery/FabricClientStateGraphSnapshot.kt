package com.minekube.craftless.driver.fabric.discovery

import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeEventNode
import com.minekube.craftless.protocol.RuntimeHandleNode
import com.minekube.craftless.protocol.RuntimeOperationNode
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSchema
import com.minekube.craftless.protocol.RuntimeSourceEvidence

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
                RuntimeResourceNode("world.block", snapshot.blockQueryAvailability()),
                RuntimeResourceNode("world.time", worldAvailability),
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
                    resource = "world.block",
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

fun fabricClientStateWorldTimeQueryOperation(
    snapshot: FabricClientStateGraphSnapshot,
    adapter: String,
): RuntimeOperationNode =
    RuntimeOperationNode(
        id = CLIENT_STATE_WORLD_TIME_QUERY_OPERATION_ID,
        resource = CLIENT_STATE_WORLD_TIME_QUERY_OPERATION_RESOURCE,
        adapter = adapter,
        result =
            RuntimeSchema(
                type = "object",
                properties =
                    mapOf(
                        "action" to RuntimeSchema("string", required = true),
                        "status" to RuntimeSchema("string", required = true),
                        "message" to RuntimeSchema("string"),
                        "data" to RuntimeSchema.objectSchema(),
                    ),
            ),
        availability = snapshot.worldAvailability(),
        sourceEvidence = listOf(snapshot.clientStateWorldEvidence()),
    )

fun RuntimeOperationNode.toFabricRuntimeEventNode(): RuntimeEventNode =
    RuntimeEventNode(
        id = id,
        resource = resource,
        payload = RuntimeSchema.objectSchema(),
        availability = availability,
        sourceEvidence = sourceEvidence,
    )

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

private fun FabricClientStateGraphSnapshot.clientStateWorldEvidence(): RuntimeSourceEvidence =
    RuntimeSourceEvidence(
        kind = "client-state",
        fingerprint = worldReason() ?: "world-available",
    )

private fun availability(reason: String?): RuntimeAvailability =
    if (reason == null) RuntimeAvailability.available() else RuntimeAvailability.unavailable(reason)

private const val CLIENT_STATE_WORLD_TIME_QUERY_OPERATION_RESOURCE = "world.time"
private const val CLIENT_STATE_WORLD_TIME_QUERY_OPERATION_ID = "$CLIENT_STATE_WORLD_TIME_QUERY_OPERATION_RESOURCE.query"
