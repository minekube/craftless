package com.minekube.craftless.driver.fabric.discovery

import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeEventNode
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSchema
import com.minekube.craftless.protocol.RuntimeSourceEvidence

fun fabricEventGraphFragment(
    sourceEvidence: List<RuntimeSourceEvidence>,
    available: Boolean,
): FabricRuntimeGraphFragment {
    val availability =
        if (available) {
            RuntimeAvailability.available()
        } else {
            RuntimeAvailability.unavailable("event-source-not-discovered")
        }
    val evidence =
        sourceEvidence.ifEmpty {
            listOf(RuntimeSourceEvidence("event-source", "events:not-discovered"))
        }
    return FabricRuntimeGraphFragment(
        resources =
            listOf(
                RuntimeResourceNode(
                    id = "event",
                    availability = availability,
                    sourceEvidence = evidence,
                ),
            ),
        events =
            fabricEventIds.map { eventId ->
                RuntimeEventNode(
                    id = eventId,
                    resource = "event",
                    payload = RuntimeSchema.objectSchema(),
                    availability = availability,
                    sourceEvidence = evidence,
                )
            },
    )
}

private val fabricEventIds =
    listOf(
        "event.lifecycle",
        "event.action",
        "event.capability",
    )
