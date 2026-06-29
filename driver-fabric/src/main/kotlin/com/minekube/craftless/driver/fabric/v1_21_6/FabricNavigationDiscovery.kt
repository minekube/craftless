package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeOperationNode
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSchema
import com.minekube.craftless.protocol.RuntimeSourceEvidence
import java.security.MessageDigest

internal object FabricNavigationOperationIds {
    const val PLAN = "navigation.plan"
    const val FOLLOW = "navigation.follow"
    const val STOP = "navigation.stop"
}

internal class FabricNavigationDiscovery(
    private val classExists: (String) -> Boolean = ::classExists,
    private val pathfinderProbe: ReflectiveFabricPathfinderProbe = ClassLoaderReflectiveFabricPathfinderProbe(),
) : FabricCapabilityProbe {
    override fun discover(context: FabricCapabilityProbeContext): FabricCapabilityGraphFragment {
        val detectedPathfinders = PATHFINDER_CLASS_CANDIDATES.filter(classExists)
        val pathfinderAvailable = detectedPathfinders.isNotEmpty() && pathfinderProbe.inspect().available
        val navigationAvailability =
            when {
                detectedPathfinders.isEmpty() -> RuntimeAvailability.unavailable("pathfinder-unavailable")
                pathfinderAvailable -> RuntimeAvailability.available()
                else -> RuntimeAvailability.unavailable(PATHFINDER_PROBE_UNAVAILABLE)
            }
        val operationAvailability =
            when {
                detectedPathfinders.isEmpty() -> RuntimeAvailability.unavailable("pathfinder-unavailable")
                pathfinderAvailable -> RuntimeAvailability.available()
                else -> RuntimeAvailability.unavailable(PATHFINDER_PROBE_UNAVAILABLE)
            }
        val sourceEvidence =
            detectedPathfinders
                .takeIf { it.isNotEmpty() }
                ?.let {
                    listOf(
                        RuntimeSourceEvidence(
                            kind = "pathfinder",
                            fingerprint = it.privateFingerprint(),
                        ),
                    )
                }.orEmpty()

        return FabricCapabilityGraphFragment(
            resources =
                listOf(
                    RuntimeResourceNode(
                        id = "navigation",
                        availability = navigationAvailability,
                        sourceEvidence = sourceEvidence,
                    ),
                ),
            operations =
                listOf(
                    navigationOperation(
                        id = FabricNavigationOperationIds.PLAN,
                        arguments = mapOf("goal" to RuntimeSchema("object", required = true)),
                        availability = operationAvailability,
                    ),
                    navigationOperation(
                        id = FabricNavigationOperationIds.FOLLOW,
                        arguments = mapOf("plan" to RuntimeSchema("object", required = true)),
                        availability = operationAvailability,
                    ),
                    navigationOperation(
                        id = FabricNavigationOperationIds.STOP,
                        availability = operationAvailability,
                    ),
                ),
        )
    }
}

private fun navigationOperation(
    id: String,
    arguments: Map<String, RuntimeSchema> = emptyMap(),
    availability: RuntimeAvailability,
): RuntimeOperationNode =
    RuntimeOperationNode(
        id = id,
        resource = "navigation",
        adapter = "navigation.default",
        arguments = arguments,
        result = RuntimeSchema.objectSchema(),
        availability = availability,
    )

private fun classExists(className: String): Boolean =
    runCatching {
        Class.forName(className, false, FabricNavigationDiscovery::class.java.classLoader)
    }.isSuccess

private fun List<String>.privateFingerprint(): String {
    val canonical = sorted().joinToString("\n")
    val digest =
        MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.encodeToByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(16)
    return "classes:$digest"
}

private val PATHFINDER_CLASS_CANDIDATES =
    listOf(
        "baritone.api.BaritoneAPI",
        "net.swarmbot.SwarmBot",
    )
