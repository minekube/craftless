package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.NavigationProgressEvent
import com.minekube.craftless.protocol.NavigationTaskRequest
import com.minekube.craftless.protocol.NavigationTaskState
import com.minekube.craftless.protocol.NavigationTaskStatus
import kotlinx.serialization.json.JsonPrimitive
import net.minecraft.entity.passive.CowEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.item.Items
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.BlockPos
import java.util.concurrent.atomic.AtomicInteger

internal interface FabricSurvivalTaskExecutor {
    fun run(request: NavigationTaskRequest): NavigationTaskStatus

    fun status(taskId: String): NavigationTaskStatus

    fun events(): List<NavigationProgressEvent>
}

internal class RecordingSurvivalExecutor(
    private val observations: FabricSurvivalObservationProvider = ReadySurvivalObservationProvider,
    private val pathfinderBackend: FabricPathfinderBackend = ReadySurvivalPathfinderBackend,
    private val nextTaskId: () -> String = ::nextSurvivalTaskId,
) : FabricSurvivalTaskExecutor {
    private val statuses = linkedMapOf<String, NavigationTaskStatus>()
    private val progressEvents = mutableListOf<NavigationProgressEvent>()

    override fun run(request: NavigationTaskRequest): NavigationTaskStatus =
        when (request.task) {
            HONEST_COW_HUNT -> startHonestCowHunt(observations.observe())
            else -> unsupportedTaskStatus()
        }

    override fun status(taskId: String): NavigationTaskStatus =
        statuses[taskId]
            ?: NavigationTaskStatus(
                id = taskId,
                state = NavigationTaskState.FAILED,
                message = "unknown-task",
            )

    override fun events(): List<NavigationProgressEvent> = progressEvents.toList()

    private fun startHonestCowHunt(observation: FabricSurvivalObservation): NavigationTaskStatus {
        val taskId = nextTaskId()
        record(
            taskId = taskId,
            type = "task.observe",
            message = "observing-survival-state",
            payload =
                mapOf(
                    "material-count" to JsonPrimitive(observation.materialSources.size),
                    "entity-count" to JsonPrimitive(observation.passiveEntities.size),
                ),
        )
        record(
            taskId = taskId,
            type = "task.inventory",
            message = if (observation.inventory.hasWeapon) "weapon-observed" else "weapon-needed",
        )
        if (!observation.inventory.hasWeapon && observation.materialSources.isEmpty()) {
            return storeStatus(
                NavigationTaskStatus(
                    id = taskId,
                    state = NavigationTaskState.FAILED,
                    message = "no-material-source",
                ),
            )
        }
        if (observation.passiveEntities.none { it.kind == COW_ENTITY_KIND }) {
            return storeStatus(
                NavigationTaskStatus(
                    id = taskId,
                    state = NavigationTaskState.FAILED,
                    message = "no-cow-observed",
                ),
            )
        }
        if (!pathfinderBackend.available()) {
            return storeStatus(
                NavigationTaskStatus(
                    id = taskId,
                    state = NavigationTaskState.FAILED,
                    message = PATHFINDER_UNAVAILABLE,
                ),
            )
        }
        record(
            taskId = taskId,
            type = "task.navigate",
            message = "navigation-ready",
        )
        val status =
            NavigationTaskStatus(
                id = taskId,
                state = NavigationTaskState.RUNNING,
                message = "ready-to-execute",
            )
        return storeStatus(status)
    }

    private fun storeStatus(status: NavigationTaskStatus): NavigationTaskStatus {
        statuses[status.id] = status
        return status
    }

    private fun record(
        taskId: String,
        type: String,
        message: String,
        payload: Map<String, JsonPrimitive> = emptyMap(),
    ) {
        progressEvents +=
            NavigationProgressEvent(
                taskId = taskId,
                type = type,
                message = message,
                payload = payload,
            )
    }

    private fun unsupportedTaskStatus(): NavigationTaskStatus =
        NavigationTaskStatus(
            id = "task:survival:unsupported:${taskIdCounter.incrementAndGet()}",
            state = NavigationTaskState.FAILED,
            message = "unsupported-task",
        )
}

internal fun interface FabricSurvivalObservationProvider {
    fun observe(): FabricSurvivalObservation
}

internal data class FabricSurvivalObservation(
    val materialSources: List<FabricSurvivalMaterialSource> = emptyList(),
    val passiveEntities: List<FabricSurvivalEntity> = emptyList(),
    val inventory: FabricSurvivalInventory = FabricSurvivalInventory(),
    val playerPosition: FabricSurvivalBlockPosition? = null,
)

internal data class FabricSurvivalMaterialSource(
    val handle: String,
    val position: FabricSurvivalBlockPosition,
)

internal data class FabricSurvivalEntity(
    val handle: String,
    val kind: String,
    val position: FabricSurvivalBlockPosition,
)

internal data class FabricSurvivalInventory(
    val hasWeapon: Boolean = false,
)

internal data class FabricSurvivalBlockPosition(
    val x: Int,
    val y: Int,
    val z: Int,
)

internal class FabricClientSurvivalObservationProvider(
    private val gateway: FabricClientGateway,
) : FabricSurvivalObservationProvider {
    override fun observe(): FabricSurvivalObservation =
        gateway.queryOnClient {
            val currentPlayer = player ?: return@queryOnClient FabricSurvivalObservation()
            val currentWorld = world ?: return@queryOnClient FabricSurvivalObservation()
            val playerPosition = currentPlayer.blockPos.toSurvivalBlockPosition()
            val materialSources =
                currentPlayer.blockPos
                    .nearbyPositions(horizontalRange = MATERIAL_SCAN_HORIZONTAL_RANGE, verticalRange = MATERIAL_SCAN_VERTICAL_RANGE)
                    .asSequence()
                    .filter { position -> currentWorld.getBlockState(position).isIn(BlockTags.LOGS) }
                    .sortedBy { position -> position.getSquaredDistance(currentPlayer.pos) }
                    .take(MATERIAL_SCAN_LIMIT)
                    .mapIndexed { index, position ->
                        FabricSurvivalMaterialSource(
                            handle = "resource:survival:material:log:%04d".format(index + 1),
                            position = position.toSurvivalBlockPosition(),
                        )
                    }.toList()
            val passiveEntities =
                currentWorld
                    .getOtherEntities(currentPlayer, currentPlayer.boundingBox.expand(ENTITY_SCAN_RADIUS)) { entity ->
                        entity is PassiveEntity && !entity.isSpectator
                    }.asSequence()
                    .sortedBy { entity -> entity.squaredDistanceTo(currentPlayer) }
                    .take(ENTITY_SCAN_LIMIT)
                    .map { entity ->
                        FabricSurvivalEntity(
                            handle = "resource:survival:entity:%04d".format(entity.id),
                            kind = if (entity is CowEntity) COW_ENTITY_KIND else PASSIVE_ENTITY_KIND,
                            position = entity.blockPos.toSurvivalBlockPosition(),
                        )
                    }.toList()
            FabricSurvivalObservation(
                materialSources = materialSources,
                passiveEntities = passiveEntities,
                inventory = FabricSurvivalInventory(hasWeapon = currentPlayer.inventory.hasSurvivalWeapon()),
                playerPosition = playerPosition,
            )
        }
}

internal class StaticSurvivalObservationProvider(
    private val observation: FabricSurvivalObservation,
) : FabricSurvivalObservationProvider {
    override fun observe(): FabricSurvivalObservation = observation
}

private object ReadySurvivalObservationProvider : FabricSurvivalObservationProvider {
    override fun observe(): FabricSurvivalObservation =
        FabricSurvivalObservation(
            materialSources =
                listOf(
                    FabricSurvivalMaterialSource(
                        handle = "resource:survival:material:log:0001",
                        position = FabricSurvivalBlockPosition(x = 1, y = 64, z = 1),
                    ),
                ),
            passiveEntities =
                listOf(
                    FabricSurvivalEntity(
                        handle = "resource:survival:entity:cow:0001",
                        kind = COW_ENTITY_KIND,
                        position = FabricSurvivalBlockPosition(x = 4, y = 64, z = 4),
                    ),
                ),
        )
}

private object ReadySurvivalPathfinderBackend : FabricPathfinderBackend {
    override fun available(): Boolean = true

    override fun plan(goal: com.minekube.craftless.protocol.NavigationGoal): FabricPathfinderPlan =
        error("RecordingSurvivalExecutor does not plan navigation in observation mode")

    override fun follow(planId: String): NavigationTaskStatus =
        error("RecordingSurvivalExecutor does not follow navigation in observation mode")

    override fun stop(): NavigationTaskStatus = error("RecordingSurvivalExecutor does not stop navigation in observation mode")

    override fun events(): List<NavigationProgressEvent> = emptyList()
}

private fun nextSurvivalTaskId(): String = "task:survival:honest-cow-hunt:%04d".format(taskIdCounter.incrementAndGet())

private fun BlockPos.nearbyPositions(
    horizontalRange: Int,
    verticalRange: Int,
): List<BlockPos> {
    val positions = mutableListOf<BlockPos>()
    for (dx in -horizontalRange..horizontalRange) {
        for (dy in -verticalRange..verticalRange) {
            for (dz in -horizontalRange..horizontalRange) {
                positions += add(dx, dy, dz)
            }
        }
    }
    return positions
}

private fun BlockPos.toSurvivalBlockPosition(): FabricSurvivalBlockPosition =
    FabricSurvivalBlockPosition(
        x = x,
        y = y,
        z = z,
    )

private fun net.minecraft.entity.player.PlayerInventory.hasSurvivalWeapon(): Boolean {
    for (slot in 0 until size()) {
        val stack = getStack(slot)
        if (stack.isOf(Items.WOODEN_SWORD) ||
            stack.isOf(Items.STONE_SWORD) ||
            stack.isOf(Items.IRON_SWORD) ||
            stack.isOf(Items.GOLDEN_SWORD) ||
            stack.isOf(Items.DIAMOND_SWORD) ||
            stack.isOf(Items.NETHERITE_SWORD)
        ) {
            return true
        }
    }
    return false
}

private const val HONEST_COW_HUNT = "task.survival.honest-cow-hunt"
private const val COW_ENTITY_KIND = "cow"
private const val PASSIVE_ENTITY_KIND = "passive"
private const val MATERIAL_SCAN_HORIZONTAL_RANGE = 8
private const val MATERIAL_SCAN_VERTICAL_RANGE = 4
private const val MATERIAL_SCAN_LIMIT = 16
private const val ENTITY_SCAN_RADIUS = 48.0
private const val ENTITY_SCAN_LIMIT = 32

private val taskIdCounter = AtomicInteger()
