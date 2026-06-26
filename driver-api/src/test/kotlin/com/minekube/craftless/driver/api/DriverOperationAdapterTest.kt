package com.minekube.craftless.driver.api

import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeOperationNode
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DriverOperationAdapterTest {
    @Test
    fun `operation adapters resolve by graph adapter key`() {
        val adapters =
            DriverOperationAdapters(
                mapOf(
                    "fabric.player-chat" to
                        DriverOperationAdapter { invocation ->
                            DriverActionResult(
                                action = invocation.operation.id,
                                status = DriverActionStatus.ACCEPTED,
                                message = "adapter ${invocation.operation.adapter} accepted ${invocation.arguments.getValue("message")}",
                            )
                        },
                ),
            )

        val result =
            adapters.invoke(
                DriverOperationInvocation(
                    clientId = "alice",
                    operation =
                        RuntimeOperationNode(
                            id = "player.chat",
                            resource = "player",
                            adapter = "fabric.player-chat",
                            availability = RuntimeAvailability.available(),
                        ),
                    arguments = mapOf("message" to JsonPrimitive("hello")),
                ),
            )

        assertEquals("player.chat", result.action)
        assertEquals(DriverActionStatus.ACCEPTED, result.status)
        assertEquals("adapter fabric.player-chat accepted \"hello\"", result.message)
    }

    @Test
    fun `operation adapters reject missing adapter keys without gameplay methods`() {
        val adapters = DriverOperationAdapters.empty()
        val operation =
            RuntimeOperationNode(
                id = "world.block.break",
                resource = "world.block",
                adapter = "fabric.world-block-break",
                availability = RuntimeAvailability.available(),
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                adapters.invoke(
                    DriverOperationInvocation(
                        clientId = "alice",
                        operation = operation,
                    ),
                )
            }

        assertEquals("operation adapter fabric.world-block-break is not registered", error.message)
        assertEquals(emptySet(), adapters.adapterKeys())
    }
}
