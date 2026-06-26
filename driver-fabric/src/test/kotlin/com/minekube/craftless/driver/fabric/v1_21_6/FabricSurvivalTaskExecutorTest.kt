package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.NavigationTaskRequest
import com.minekube.craftless.protocol.NavigationTaskState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FabricSurvivalTaskExecutorTest {
    @Test
    fun `executor rejects unknown tasks without static shortcut actions`() {
        val executor = RecordingSurvivalExecutor()

        val result = executor.run(NavigationTaskRequest(task = "task.survival.unknown"))

        assertEquals(NavigationTaskState.FAILED, result.state)
        assertEquals("unsupported-task", result.message)
        assertFalse(result.toString().contains("kill.cow", ignoreCase = true))
        assertFalse(result.toString().contains("craft.sword", ignoreCase = true))
    }

    @Test
    fun `honest cow hunt starts with observable task status and progress`() {
        val executor = RecordingSurvivalExecutor()

        val result = executor.run(NavigationTaskRequest(task = "task.survival.honest-cow-hunt"))

        assertEquals(NavigationTaskState.RUNNING, result.state)
        assertEquals(result.id, executor.status(result.id).id)
        assertTrue(executor.events().any { it.type == "task.observe" })
        assertTrue(executor.events().none { it.toString().contains("baritone", ignoreCase = true) })
    }

    @Test
    fun `honest cow hunt fails honestly when no material source is observed`() {
        val executor =
            RecordingSurvivalExecutor(
                observations =
                    StaticSurvivalObservationProvider(
                        FabricSurvivalObservation(
                            materialSources = emptyList(),
                            passiveEntities = listOf(cow()),
                        ),
                    ),
                pathfinderBackend = AvailableSurvivalPathfinderBackend,
            )

        val result = executor.run(NavigationTaskRequest(task = "task.survival.honest-cow-hunt"))

        assertEquals(NavigationTaskState.FAILED, result.state)
        assertEquals("no-material-source", result.message)
    }

    @Test
    fun `honest cow hunt fails honestly when no cow is observed`() {
        val executor =
            RecordingSurvivalExecutor(
                observations =
                    StaticSurvivalObservationProvider(
                        FabricSurvivalObservation(
                            materialSources = listOf(materialSource()),
                            passiveEntities = emptyList(),
                        ),
                    ),
                pathfinderBackend = AvailableSurvivalPathfinderBackend,
            )

        val result = executor.run(NavigationTaskRequest(task = "task.survival.honest-cow-hunt"))

        assertEquals(NavigationTaskState.FAILED, result.state)
        assertEquals("no-cow-observed", result.message)
    }

    @Test
    fun `honest cow hunt fails honestly when pathfinder is unavailable`() {
        val executor =
            RecordingSurvivalExecutor(
                observations =
                    StaticSurvivalObservationProvider(
                        FabricSurvivalObservation(
                            materialSources = listOf(materialSource()),
                            passiveEntities = listOf(cow()),
                        ),
                    ),
                pathfinderBackend = UnavailableFabricPathfinderBackend,
            )

        val result = executor.run(NavigationTaskRequest(task = "task.survival.honest-cow-hunt"))

        assertEquals(NavigationTaskState.FAILED, result.state)
        assertEquals("pathfinder-unavailable", result.message)
    }

    @Test
    fun `honest cow hunt records Craftless progress events without backend names`() {
        val executor =
            RecordingSurvivalExecutor(
                observations =
                    StaticSurvivalObservationProvider(
                        FabricSurvivalObservation(
                            materialSources = listOf(materialSource()),
                            passiveEntities = listOf(cow()),
                            inventory = FabricSurvivalInventory(hasWeapon = false),
                        ),
                    ),
                pathfinderBackend = AvailableSurvivalPathfinderBackend,
                nextTaskId = { "task:survival:honest-cow-hunt:0001" },
            )

        val result = executor.run(NavigationTaskRequest(task = "task.survival.honest-cow-hunt"))

        assertEquals(NavigationTaskState.RUNNING, result.state)
        assertEquals("ready-to-execute", result.message)
        assertNotNull(executor.status("task:survival:honest-cow-hunt:0001"))
        assertEquals(
            listOf("task.observe", "task.inventory", "task.navigate"),
            executor.events().map { it.type },
        )
        assertTrue(executor.events().none { it.toString().contains("baritone", ignoreCase = true) })
        assertTrue(executor.events().none { it.toString().contains("swarmbot", ignoreCase = true) })
    }
}

private fun materialSource(): FabricSurvivalMaterialSource =
    FabricSurvivalMaterialSource(
        handle = "resource:survival:material:log:0001",
        position = FabricSurvivalBlockPosition(x = 1, y = 64, z = 1),
    )

private fun cow(): FabricSurvivalEntity =
    FabricSurvivalEntity(
        handle = "resource:survival:entity:cow:0001",
        kind = "cow",
        position = FabricSurvivalBlockPosition(x = 4, y = 64, z = 4),
    )

private object AvailableSurvivalPathfinderBackend : FabricPathfinderBackend {
    override fun available(): Boolean = true

    override fun plan(goal: com.minekube.craftless.protocol.NavigationGoal): FabricPathfinderPlan = error("not used by observation tests")

    override fun follow(planId: String): com.minekube.craftless.protocol.NavigationTaskStatus = error("not used by observation tests")

    override fun stop(): com.minekube.craftless.protocol.NavigationTaskStatus = error("not used by observation tests")

    override fun events(): List<com.minekube.craftless.protocol.NavigationProgressEvent> = emptyList()
}
