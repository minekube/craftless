package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.NavigationGoal
import com.minekube.craftless.protocol.NavigationPlan
import com.minekube.craftless.protocol.NavigationProgressEvent
import com.minekube.craftless.protocol.NavigationTaskState
import com.minekube.craftless.protocol.NavigationTaskStatus

internal interface FabricPathfinderBackend {
    fun available(): Boolean

    fun plan(goal: NavigationGoal): FabricPathfinderPlan

    fun follow(planId: String): NavigationTaskStatus

    fun stop(): NavigationTaskStatus

    fun events(): List<NavigationProgressEvent>
}

internal data class FabricPathfinderPlan(
    val id: String,
    val goal: NavigationGoal,
    val status: NavigationTaskStatus,
    val plan: NavigationPlan? = null,
)

internal object UnavailableFabricPathfinderBackend : FabricPathfinderBackend {
    override fun available(): Boolean = false

    override fun plan(goal: NavigationGoal): FabricPathfinderPlan =
        FabricPathfinderPlan(
            id = "navigation.plan.unavailable.0001",
            goal = goal,
            status =
                NavigationTaskStatus(
                    id = "task:navigation:unavailable",
                    state = NavigationTaskState.FAILED,
                    message = PATHFINDER_UNAVAILABLE,
                ),
        )

    override fun follow(planId: String): NavigationTaskStatus =
        NavigationTaskStatus(
            id = "task:navigation:unavailable",
            state = NavigationTaskState.FAILED,
            message = PATHFINDER_UNAVAILABLE,
        )

    override fun stop(): NavigationTaskStatus =
        NavigationTaskStatus(
            id = "task:navigation:unavailable",
            state = NavigationTaskState.FAILED,
            message = PATHFINDER_UNAVAILABLE,
        )

    override fun events(): List<NavigationProgressEvent> = emptyList()
}

internal const val PATHFINDER_UNAVAILABLE = "pathfinder-unavailable"
