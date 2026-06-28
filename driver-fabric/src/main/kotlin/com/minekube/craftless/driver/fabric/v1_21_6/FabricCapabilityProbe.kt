package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import com.minekube.craftless.driver.fabric.discovery.FabricClientStateGraphSnapshot
import com.minekube.craftless.driver.fabric.discovery.FabricRuntimeGraphFragment
import com.minekube.craftless.driver.fabric.discovery.fabricClientStateGraphFragment
import com.minekube.craftless.driver.fabric.discovery.fabricClientStateWorldTimeQueryOperation
import com.minekube.craftless.driver.fabric.discovery.fabricEventGraphFragment
import com.minekube.craftless.driver.fabric.discovery.fabricRegistryGraphFragment
import com.minekube.craftless.driver.fabric.discovery.fabricRuntimeGraph
import com.minekube.craftless.driver.fabric.discovery.fabricRuntimeResourceNode
import com.minekube.craftless.driver.fabric.runtime.FabricCompatibilityLane
import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeCapabilityGraph
import com.minekube.craftless.protocol.RuntimeOperationNode
import com.minekube.craftless.protocol.RuntimeSourceEvidence

internal fun interface FabricCapabilityDiscovery {
    fun discover(context: FabricCapabilityProbeContext): RuntimeCapabilityGraph
}

internal fun interface FabricCapabilityProbe {
    fun discover(context: FabricCapabilityProbeContext): FabricCapabilityGraphFragment
}

internal typealias FabricCapabilityGraphFragment = FabricRuntimeGraphFragment

internal data class FabricCapabilityProbeContext(
    val clientId: String,
    val modeId: String,
    val gateway: FabricClientGateway?,
    val runtimeMetadata: DriverRuntimeMetadata = DriverRuntimeMetadata.runtimeAdapter(),
    val compatibilityLane: FabricCompatibilityLane? = null,
)

internal data class FabricClientCapabilitySnapshot(
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
        fun disconnected(): FabricClientCapabilitySnapshot =
            FabricClientCapabilitySnapshot(
                connected = false,
                player = false,
                inventory = false,
                camera = false,
                interactionManager = false,
                world = false,
            )
    }
}

internal fun defaultFabricCapabilityDiscovery(
    probes: List<FabricCapabilityProbe> = defaultFabricCapabilityProbes(),
): FabricCapabilityDiscovery =
    FabricCapabilityDiscovery { context ->
        fabricRuntimeGraph(
            clientId = context.clientId,
            fragments = probes.map { probe -> probe.discover(context) },
        )
    }

private fun defaultFabricCapabilityProbes(): List<FabricCapabilityProbe> =
    listOf(
        FabricRuntimeMetadataCapabilityProbe,
        FabricRegistrySummaryCapabilityProbe,
        FabricEventSourceCapabilityProbe,
        FabricClientStateCapabilityProbe,
        FabricNavigationDiscovery(),
    )

internal object FabricRuntimeMetadataCapabilityProbe : FabricCapabilityProbe {
    override fun discover(context: FabricCapabilityProbeContext): FabricCapabilityGraphFragment =
        FabricCapabilityGraphFragment(
            resources =
                listOf(
                    fabricRuntimeResourceNode(
                        metadata = context.runtimeMetadata,
                        sourceEvidence = context.compatibilityLane.sourceEvidence(),
                    ),
                ),
        )
}

private fun FabricCompatibilityLane?.sourceEvidence(): List<RuntimeSourceEvidence> = this?.sourceEvidence().orEmpty()

internal object FabricRegistrySummaryCapabilityProbe : FabricCapabilityProbe {
    override fun discover(context: FabricCapabilityProbeContext): FabricCapabilityGraphFragment =
        fabricRegistryGraphFragment(
            metadata = context.runtimeMetadata,
            available = true,
        )
}

internal object FabricEventSourceCapabilityProbe : FabricCapabilityProbe {
    override fun discover(context: FabricCapabilityProbeContext): FabricCapabilityGraphFragment {
        val eventSourceEvidence =
            listOf(RuntimeSourceEvidence("event-source", "driver:${context.runtimeMetadata.driverVersion}")) +
                FabricEventHooks.sourceEvidence() +
                FabricEventCallbacks.sourceEvidence()
        return fabricEventGraphFragment(
            sourceEvidence = eventSourceEvidence,
            available = true,
        )
    }
}

internal object FabricClientStateCapabilityProbe : FabricCapabilityProbe {
    override fun discover(context: FabricCapabilityProbeContext): FabricCapabilityGraphFragment {
        val gateway = context.gateway
        val capabilities =
            if (gateway == null || !gateway.isConnected()) {
                FabricClientCapabilitySnapshot.disconnected()
            } else {
                gateway.queryOnClient {
                    val currentPlayer = player
                    FabricClientCapabilitySnapshot(
                        connected = networkHandler != null && currentPlayer != null,
                        player = currentPlayer != null,
                        inventory = currentPlayer?.inventory != null,
                        camera = cameraEntity != null || currentPlayer != null,
                        interactionManager = interactionManager != null,
                        world = world != null,
                        recipes = currentPlayer?.recipeBook != null && networkHandler?.recipeManager != null,
                        recipeCrafting = interactionManager != null && currentPlayer?.currentScreenHandler != null,
                    )
                }
            }
        val screenOpen =
            if (gateway == null || !gateway.isConnected()) {
                false
            } else {
                gateway.queryOnClient { currentScreen != null }
            }
        val clientStateFragment = fabricClientStateGraphFragment(capabilities.toGraphSnapshot())
        val operations =
            fabricBootstrapOperationDefinitions().map { definition ->
                definition.toRuntimeOperation(
                    capabilities.bootstrapAvailability(
                        kind = definition.availability,
                        screenOpen = screenOpen,
                    ),
                )
            } + capabilities.clientStateOperations()

        return FabricCapabilityGraphFragment(
            resources = clientStateFragment.resources,
            operations = operations,
            handles = clientStateFragment.handles,
            events = operations.map { operation -> operation.toFabricEventNode() },
        )
    }
}

private fun FabricClientCapabilitySnapshot.toGraphSnapshot(): FabricClientStateGraphSnapshot =
    FabricClientStateGraphSnapshot(
        connected = connected,
        player = player,
        inventory = inventory,
        camera = camera,
        interactionManager = interactionManager,
        world = world,
        recipes = recipes,
        recipeCrafting = recipeCrafting,
    )

private fun FabricClientCapabilitySnapshot.playerAvailability(): RuntimeAvailability = availability(playerReason())

private fun FabricClientCapabilitySnapshot.inventoryAvailability(): RuntimeAvailability = availability(inventoryReason())

private fun FabricClientCapabilitySnapshot.recipeQueryAvailability(): RuntimeAvailability = availability(recipeQueryReason())

private fun FabricClientCapabilitySnapshot.recipeCraftAvailability(): RuntimeAvailability = availability(recipeCraftReason())

private fun FabricClientCapabilitySnapshot.cameraAvailability(): RuntimeAvailability = availability(cameraReason())

private fun FabricClientCapabilitySnapshot.worldAvailability(): RuntimeAvailability = availability(worldReason())

private fun FabricClientCapabilitySnapshot.entityAvailability(): RuntimeAvailability = availability(entityReason())

private fun FabricClientCapabilitySnapshot.entityAttackAvailability(): RuntimeAvailability = availability(entityAttackReason())

private fun FabricClientCapabilitySnapshot.blockQueryAvailability(): RuntimeAvailability = availability(blockQueryReason())

private fun FabricClientCapabilitySnapshot.blockBreakAvailability(): RuntimeAvailability = availability(blockBreakReason())

private fun FabricClientCapabilitySnapshot.blockInteractAvailability(): RuntimeAvailability = availability(blockInteractReason())

private fun FabricClientCapabilitySnapshot.bootstrapAvailability(
    kind: FabricBootstrapOperationAvailabilityKind,
    screenOpen: Boolean,
): RuntimeAvailability =
    when (kind) {
        FabricBootstrapOperationAvailabilityKind.PLAYER -> playerAvailability()
        FabricBootstrapOperationAvailabilityKind.CAMERA -> cameraAvailability()
        FabricBootstrapOperationAvailabilityKind.INVENTORY -> inventoryAvailability()
        FabricBootstrapOperationAvailabilityKind.RECIPE_QUERY -> recipeQueryAvailability()
        FabricBootstrapOperationAvailabilityKind.RECIPE_CRAFT -> recipeCraftAvailability()
        FabricBootstrapOperationAvailabilityKind.ENTITY -> entityAvailability()
        FabricBootstrapOperationAvailabilityKind.ENTITY_ATTACK -> entityAttackAvailability()
        FabricBootstrapOperationAvailabilityKind.BLOCK_QUERY -> blockQueryAvailability()
        FabricBootstrapOperationAvailabilityKind.BLOCK_BREAK -> blockBreakAvailability()
        FabricBootstrapOperationAvailabilityKind.BLOCK_INTERACT -> blockInteractAvailability()
        FabricBootstrapOperationAvailabilityKind.SCREEN -> RuntimeAvailability.available()
        FabricBootstrapOperationAvailabilityKind.SCREEN_CLOSE ->
            if (screenOpen) RuntimeAvailability.available() else RuntimeAvailability.unavailable("screen-not-open")
    }

private fun FabricClientCapabilitySnapshot.playerReason(): String? =
    when {
        !connected -> "client-not-connected"
        !player -> "player-unavailable"
        else -> null
    }

private fun FabricClientCapabilitySnapshot.inventoryReason(): String? =
    when {
        !connected -> "client-not-connected"
        !inventory -> "inventory-unavailable"
        else -> null
    }

private fun FabricClientCapabilitySnapshot.recipeQueryReason(): String? =
    playerReason()
        ?: inventoryReason()
        ?: if (!recipes) "recipe-discovery-unavailable" else null

private fun FabricClientCapabilitySnapshot.recipeCraftReason(): String? =
    recipeQueryReason()
        ?: when {
            !interactionManager -> "interaction-manager-unavailable"
            !recipeCrafting -> "recipe-context-unavailable"
            else -> null
        }

private fun FabricClientCapabilitySnapshot.cameraReason(): String? =
    when {
        !connected -> "client-not-connected"
        !camera -> "camera-unavailable"
        else -> null
    }

private fun FabricClientCapabilitySnapshot.worldReason(): String? =
    when {
        !connected -> "client-not-connected"
        !world -> "world-unavailable"
        else -> null
    }

private fun FabricClientCapabilitySnapshot.entityReason(): String? =
    playerReason()
        ?: worldReason()

private fun FabricClientCapabilitySnapshot.entityAttackReason(): String? =
    playerReason()
        ?: worldReason()
        ?: when {
            !interactionManager -> "interaction-unavailable"
            else -> null
        }

private fun FabricClientCapabilitySnapshot.blockQueryReason(): String? =
    playerReason()
        ?: worldReason()

private fun FabricClientCapabilitySnapshot.blockBreakReason(): String? =
    worldReason()
        ?: cameraReason()
        ?: when {
            !interactionManager -> "interaction-unavailable"
            else -> null
        }

private fun FabricClientCapabilitySnapshot.blockInteractReason(): String? =
    playerReason()
        ?: worldReason()
        ?: cameraReason()
        ?: when {
            !interactionManager -> "interaction-unavailable"
            else -> null
        }

private fun FabricClientCapabilitySnapshot.clientStateOperations(): List<RuntimeOperationNode> =
    listOf(
        fabricClientStateWorldTimeQueryOperation(
            snapshot = toGraphSnapshot(),
            adapter =
                requireNotNull(fabricBootstrapOperationAdapterKey(FabricBootstrapOperationIds.WORLD_TIME_QUERY)) {
                    "missing bootstrap operation adapter for ${FabricBootstrapOperationIds.WORLD_TIME_QUERY}"
                },
        ),
    )

private fun availability(reason: String?): RuntimeAvailability =
    if (reason == null) RuntimeAvailability.available() else RuntimeAvailability.unavailable(reason)
