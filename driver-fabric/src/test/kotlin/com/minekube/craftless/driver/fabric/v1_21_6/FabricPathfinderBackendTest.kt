package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.NavigationGoal
import com.minekube.craftless.protocol.NavigationTaskState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FabricPathfinderBackendTest {
    @Test
    fun `recording backend plans follows stops and emits craftless progress`() {
        val backend = RecordingFabricPathfinderBackend(available = true)
        val goal =
            NavigationGoal(
                kind = "block",
                position = mapOf("x" to 1.0, "y" to 64.0, "z" to 1.0),
            )

        val plan = backend.plan(goal)
        val follow = backend.follow(plan.id)
        val stop = backend.stop()
        val events = backend.events()

        assertTrue(plan.id.startsWith("navigation.plan."))
        assertEquals(goal, plan.goal)
        assertEquals(NavigationTaskState.RUNNING, follow.state)
        assertEquals(NavigationTaskState.CANCELLED, stop.state)
        assertTrue(events.any { it.type == "navigation.plan" })
        assertTrue(events.any { it.type == "navigation.follow" })
        assertTrue(events.any { it.type == "navigation.stop" })
        assertFalse(events.joinToString().contains("baritone", ignoreCase = true))
        assertFalse(events.joinToString().contains("swarmbot", ignoreCase = true))
    }

    @Test
    fun `recording backend reports unavailable state without throwing`() {
        val backend = RecordingFabricPathfinderBackend(available = false)
        val goal =
            NavigationGoal(
                kind = "block",
                position = mapOf("x" to 1.0, "y" to 64.0, "z" to 1.0),
            )

        val plan = backend.plan(goal)

        assertEquals(NavigationTaskState.FAILED, plan.status.state)
        assertEquals("pathfinder-unavailable", plan.status.message)
    }
}

private class RecordingFabricPathfinderBackend(
    private val available: Boolean,
) : FabricPathfinderBackend {
    private val events = mutableListOf<com.minekube.craftless.protocol.NavigationProgressEvent>()
    private var counter = 0

    override fun available(): Boolean = available

    override fun plan(goal: NavigationGoal): FabricPathfinderPlan {
        counter += 1
        val planId = "navigation.plan.recording.%04d".format(counter)
        val status =
            if (available) {
                com.minekube.craftless.protocol.NavigationTaskStatus(
                    id = "task:navigation:%04d".format(counter),
                    state = NavigationTaskState.PENDING,
                    message = "planned",
                )
            } else {
                com.minekube.craftless.protocol.NavigationTaskStatus(
                    id = "task:navigation:%04d".format(counter),
                    state = NavigationTaskState.FAILED,
                    message = PATHFINDER_UNAVAILABLE,
                )
            }
        events +=
            com.minekube.craftless.protocol.NavigationProgressEvent(
                taskId = status.id,
                type = "navigation.plan",
                message = status.message ?: "planned",
            )
        return FabricPathfinderPlan(
            id = planId,
            goal = goal,
            status = status,
        )
    }

    override fun follow(planId: String): com.minekube.craftless.protocol.NavigationTaskStatus {
        val status =
            com.minekube.craftless.protocol.NavigationTaskStatus(
                id = "task:navigation:%04d".format(counter),
                state = if (available) NavigationTaskState.RUNNING else NavigationTaskState.FAILED,
                message = if (available) "following" else PATHFINDER_UNAVAILABLE,
            )
        events +=
            com.minekube.craftless.protocol.NavigationProgressEvent(
                taskId = status.id,
                type = "navigation.follow",
                message = status.message ?: "following",
            )
        return status
    }

    override fun stop(): com.minekube.craftless.protocol.NavigationTaskStatus {
        val status =
            com.minekube.craftless.protocol.NavigationTaskStatus(
                id = "task:navigation:%04d".format(counter),
                state = if (available) NavigationTaskState.CANCELLED else NavigationTaskState.FAILED,
                message = if (available) "stopped" else PATHFINDER_UNAVAILABLE,
            )
        events +=
            com.minekube.craftless.protocol.NavigationProgressEvent(
                taskId = status.id,
                type = "navigation.stop",
                message = status.message ?: "stopped",
            )
        return status
    }

    override fun events(): List<com.minekube.craftless.protocol.NavigationProgressEvent> = events.toList()
}
