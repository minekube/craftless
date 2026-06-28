package com.minekube.craftless.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationModelsTest {
    @Test
    fun `navigation models serialize craftless owned goals plans tasks and progress events`() {
        val goal =
            NavigationGoal(
                kind = "block",
                position = mapOf("x" to 10.0, "y" to 64.0, "z" to -4.0),
                radius = 2.0,
            )
        val segment =
            NavigationRouteSegment(
                index = 0,
                from = goal.copy(position = mapOf("x" to 0.0, "y" to 64.0, "z" to 0.0)),
                to = goal,
                cost = 12.5,
                operations = listOf("player.move", "world.block.break"),
            )
        val plan =
            NavigationPlan(
                id = "navigation.plan.alice.0001",
                goal = goal,
                engine = "navigation.default",
                segments = listOf(segment),
            )
        val task =
            NavigationTaskRequest(
                task = "task.generic.obtain-materials",
                args =
                    mapOf(
                        "target" to JsonPrimitive("logs"),
                    ),
            )
        val status =
            NavigationTaskStatus(
                id = "task:alice:0001",
                state = "running",
                message = "collecting logs",
            )
        val event =
            NavigationProgressEvent(
                taskId = status.id,
                type = "task.progress",
                message = "selected nearby tree",
                payload =
                    mapOf(
                        "distance" to JsonPrimitive(8),
                    ),
            )

        val encodedPlan = Json.encodeToString(NavigationPlan.serializer(), plan)
        val encodedTask = Json.encodeToString(NavigationTaskRequest.serializer(), task)
        val encodedEvent = Json.encodeToString(NavigationProgressEvent.serializer(), event)

        assertTrue(encodedPlan.contains("navigation.plan.alice.0001"))
        assertTrue(encodedPlan.contains("world.block.break"))
        assertTrue(encodedTask.contains("task.generic.obtain-materials"))
        assertTrue(encodedEvent.contains("task.progress"))
        assertFalse(encodedPlan.contains("baritone", ignoreCase = true))
    }

    @Test
    fun `navigation models reject backend and raw implementation names`() {
        assertFailsWith<IllegalArgumentException> {
            NavigationEngineDescriptor(id = "baritone.primary", displayName = "Baritone")
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationEngineDescriptor(id = "navigation.baritone", displayName = "Baritone")
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationEngineDescriptor(id = "navigation.default", displayName = "Baritone")
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationTaskRequest(task = "baritone.goto", args = emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationTaskRequest(task = "task.swarmbot.parkour", args = emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationTaskRequest(task = "task.survival.obtain-weapon", args = emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationProgressEvent(
                taskId = "task:alice:0001",
                type = "minecraft.entity",
                message = "raw entity event",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            NavigationProgressEvent(
                taskId = "task:alice:0001",
                type = "task.survival.progress",
                message = "removed survival task event",
            )
        }
    }

    @Test
    fun `navigation protocol calls survival namespace removed not legacy`() {
        val root = repositoryRoot()
        val files =
            listOf(
                "protocol/src/main/kotlin/com/minekube/craftless/protocol/NavigationModels.kt",
                "protocol/src/test/kotlin/com/minekube/craftless/protocol/NavigationModelsTest.kt",
            )
        val forbidden = "legacy " + "survival"
        val violations =
            files.filter { relative ->
                Files.readString(root.resolve(relative)).contains(forbidden, ignoreCase = true)
            }

        assertTrue(
            violations.isEmpty(),
            "Navigation protocol source/tests must call task.survival.* removed, not legacy:\n" +
                violations.joinToString("\n"),
        )
    }

    @Test
    fun `navigation task status validates stable state and nonblank messages`() {
        val status =
            NavigationTaskStatus(
                id = "task:alice:0002",
                state = NavigationTaskState.RUNNING,
                message = "moving to tree",
                data =
                    buildJsonObject {
                        put("step", "observe.logs")
                    },
            )

        assertEquals(NavigationTaskState.RUNNING, status.state)
        assertEquals("moving to tree", status.message)

        assertFailsWith<IllegalArgumentException> {
            status.copy(id = "bad id")
        }
        assertFailsWith<IllegalArgumentException> {
            status.copy(state = "minecraft.running")
        }
        assertFailsWith<IllegalArgumentException> {
            status.copy(message = "")
        }
    }
}

private fun repositoryRoot(): Path {
    var current = Path.of("").toAbsolutePath().normalize()
    while (!Files.exists(current.resolve("settings.gradle.kts"))) {
        current = requireNotNull(current.parent) { "repository root not found" }
    }
    return current
}
