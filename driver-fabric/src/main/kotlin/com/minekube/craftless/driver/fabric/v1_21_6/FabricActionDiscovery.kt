package com.minekube.craftless.driver.fabric.v1_21_6

import com.minekube.craftless.driver.api.DriverActionAvailability
import com.minekube.craftless.driver.api.DriverActionDescriptor
import com.minekube.craftless.driver.api.DriverActionSource

internal fun interface FabricActionDiscovery {
    fun discover(context: FabricActionDiscoveryContext): List<FabricDiscoveredAction>
}

internal fun interface FabricActionProbe {
    fun discover(context: FabricActionDiscoveryContext): List<FabricDiscoveredAction>
}

internal data class FabricActionDiscoveryContext(
    val clientId: String,
    val modeId: String,
    val gateway: FabricClientGateway?,
    val bindings: Map<String, FabricActionBinding>,
)

internal data class FabricDiscoveredAction(
    val descriptor: DriverActionDescriptor,
    val binding: FabricActionBinding? = null,
) {
    init {
        if (binding == null) {
            require(
                descriptor.source == DriverActionSource.RUNTIME_PROBE &&
                    descriptor.availability == DriverActionAvailability.UNAVAILABLE,
            ) {
                "discovered action ${descriptor.id} must have a binding or unavailable runtime-probe metadata"
            }
        }
        if (binding != null) {
            require(descriptor.availability == DriverActionAvailability.AVAILABLE) {
                "binding-backed action ${descriptor.id} must be available"
            }
        }
        if (descriptor.source == DriverActionSource.RUNTIME_PROBE) {
            require(descriptor.availability == DriverActionAvailability.UNAVAILABLE || binding != null) {
                "runtime-probe action ${descriptor.id} must have a binding or unavailable reason"
            }
        }
    }
}

internal fun defaultFabricActionDiscovery(probes: List<FabricActionProbe> = defaultFabricActionProbes()): FabricActionDiscovery =
    FabricActionDiscovery { context ->
        probes
            .flatMap { probe -> probe.discover(context) }
            .also { actions -> actions.requireUniqueActionIds() }
    }

private fun defaultFabricActionProbes(): List<FabricActionProbe> =
    listOf(
        BindingBackedFabricActionProbe,
        ScreenFabricActionProbe,
        ConnectedClientFabricActionProbe,
    )

private object BindingBackedFabricActionProbe : FabricActionProbe {
    override fun discover(context: FabricActionDiscoveryContext): List<FabricDiscoveredAction> =
        context.bindings.values.map { binding ->
            FabricDiscoveredAction(
                descriptor =
                    binding.descriptor.copy(
                        source = DriverActionSource.BINDING,
                        availability = DriverActionAvailability.AVAILABLE,
                        availabilityReason = null,
                    ),
                binding = binding,
            )
        }
}

private object ScreenFabricActionProbe : FabricActionProbe {
    override fun discover(context: FabricActionDiscoveryContext): List<FabricDiscoveredAction> =
        if (context.gateway == null) {
            emptyList()
        } else {
            listOf(
                FabricDiscoveredAction(
                    descriptor = FabricScreenQueryActionBinding.descriptor,
                    binding = FabricScreenQueryActionBinding,
                ),
                context.discoverScreenCloseAction(),
            )
        }
}

private object ConnectedClientFabricActionProbe : FabricActionProbe {
    override fun discover(context: FabricActionDiscoveryContext): List<FabricDiscoveredAction> = context.probeConnectedClientActions()
}

private fun FabricActionDiscoveryContext.probeConnectedClientActions(): List<FabricDiscoveredAction> {
    val gateway = gateway ?: return emptyList()
    return if (gateway.isConnected()) {
        listOf(
            FabricDiscoveredAction(
                descriptor = FabricPlayerQueryActionBinding.descriptor,
                binding = FabricPlayerQueryActionBinding,
            ),
            FabricDiscoveredAction(
                descriptor = FabricPlayerLookActionBinding.descriptor,
                binding = FabricPlayerLookActionBinding,
            ),
            FabricDiscoveredAction(
                descriptor = FabricPlayerRaycastActionBinding.descriptor,
                binding = FabricPlayerRaycastActionBinding,
            ),
            FabricDiscoveredAction(
                descriptor = FabricInventoryQueryActionBinding.descriptor,
                binding = FabricInventoryQueryActionBinding,
            ),
            FabricDiscoveredAction(
                descriptor = FabricInventoryEquipActionBinding.descriptor,
                binding = FabricInventoryEquipActionBinding,
            ),
            FabricDiscoveredAction(
                descriptor = FabricWorldBlockBreakActionBinding.descriptor,
                binding = FabricWorldBlockBreakActionBinding,
            ),
        )
    } else {
        listOf(
            FabricDiscoveredAction(
                descriptor = unavailablePlayerQueryDescriptor(),
            ),
            FabricDiscoveredAction(
                descriptor = unavailablePlayerLookDescriptor(),
            ),
            FabricDiscoveredAction(
                descriptor = unavailableRaycastDescriptor(),
            ),
            FabricDiscoveredAction(
                descriptor = unavailableInventoryQueryDescriptor(),
            ),
            FabricDiscoveredAction(
                descriptor = unavailableInventoryEquipDescriptor(),
            ),
            FabricDiscoveredAction(
                descriptor = unavailableWorldBlockBreakDescriptor(),
            ),
        )
    }
}

private fun List<FabricDiscoveredAction>.requireUniqueActionIds() {
    val duplicateAction =
        groupBy { it.descriptor.id }
            .entries
            .firstOrNull { (_, matches) -> matches.size > 1 }
    if (duplicateAction != null) {
        throw IllegalArgumentException("duplicate discovered Fabric action id ${duplicateAction.key}")
    }
}

private fun unavailablePlayerQueryDescriptor(): DriverActionDescriptor =
    fabricPlayerQueryDescriptor().copy(
        source = DriverActionSource.RUNTIME_PROBE,
        availability = DriverActionAvailability.UNAVAILABLE,
        availabilityReason = "client-not-connected",
    )

private fun unavailablePlayerLookDescriptor(): DriverActionDescriptor =
    fabricPlayerLookDescriptor().copy(
        source = DriverActionSource.RUNTIME_PROBE,
        availability = DriverActionAvailability.UNAVAILABLE,
        availabilityReason = "client-not-connected",
    )

private fun unavailableRaycastDescriptor(): DriverActionDescriptor =
    fabricRaycastDescriptor().copy(
        source = DriverActionSource.RUNTIME_PROBE,
        availability = DriverActionAvailability.UNAVAILABLE,
        availabilityReason = "client-not-connected",
    )

private fun unavailableInventoryQueryDescriptor(): DriverActionDescriptor =
    fabricInventoryQueryDescriptor().copy(
        source = DriverActionSource.RUNTIME_PROBE,
        availability = DriverActionAvailability.UNAVAILABLE,
        availabilityReason = "client-not-connected",
    )

private fun unavailableInventoryEquipDescriptor(): DriverActionDescriptor =
    fabricInventoryEquipDescriptor().copy(
        source = DriverActionSource.RUNTIME_PROBE,
        availability = DriverActionAvailability.UNAVAILABLE,
        availabilityReason = "client-not-connected",
    )

private fun unavailableWorldBlockBreakDescriptor(): DriverActionDescriptor =
    fabricWorldBlockBreakDescriptor().copy(
        source = DriverActionSource.RUNTIME_PROBE,
        availability = DriverActionAvailability.UNAVAILABLE,
        availabilityReason = "client-not-connected",
    )

private fun FabricActionDiscoveryContext.discoverScreenCloseAction(): FabricDiscoveredAction {
    val gateway = requireNotNull(gateway)
    return if (gateway.queryOnClient { currentScreen != null }) {
        FabricDiscoveredAction(
            descriptor = FabricScreenCloseActionBinding.descriptor,
            binding = FabricScreenCloseActionBinding,
        )
    } else {
        FabricDiscoveredAction(
            descriptor =
                fabricScreenCloseDescriptor().copy(
                    source = DriverActionSource.RUNTIME_PROBE,
                    availability = DriverActionAvailability.UNAVAILABLE,
                    availabilityReason = "screen-not-open",
                ),
        )
    }
}
