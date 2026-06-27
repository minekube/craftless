package com.minekube.craftless.driver.fabric.runtime

internal interface FabricRuntimeProvider {
    val id: String

    fun support(identity: FabricRuntimeIdentity): FabricRuntimeSupport

    fun createAccess(identity: FabricRuntimeIdentity): FabricRuntimeAccess
}

internal data class FabricRuntimeSupport(
    val state: FabricRuntimeSupportState,
    val reason: String,
) {
    init {
        require(reason.matches(machineCode)) { "runtime support reason must be a machine-readable Craftless code" }
    }

    companion object {
        fun supported(reason: String = "supported"): FabricRuntimeSupport =
            FabricRuntimeSupport(FabricRuntimeSupportState.SUPPORTED, reason)

        fun unsupported(reason: String): FabricRuntimeSupport = FabricRuntimeSupport(FabricRuntimeSupportState.UNSUPPORTED, reason)
    }
}

internal enum class FabricRuntimeSupportState {
    SUPPORTED,
    UNSUPPORTED,
}

internal data class FabricRuntimeProviderSelection(
    val provider: FabricRuntimeProvider,
    val support: FabricRuntimeSupport,
    val access: FabricRuntimeAccess,
)

internal fun selectFabricRuntimeProvider(
    identity: FabricRuntimeIdentity,
    providers: List<FabricRuntimeProvider>,
    matrix: FabricCompatibilityMatrix = defaultFabricCompatibilityMatrix(),
): FabricRuntimeProviderSelection {
    val lane = matrix.resolve(identity)
    if (lane.status != FabricCompatibilityStatus.SUPPORTED) {
        throw IllegalArgumentException(
            "no Fabric runtime provider supports Minecraft ${identity.gameVersion}: " +
                "${lane.providerId}:${lane.unsupportedReason ?: lane.status.name.lowercase()}",
        )
    }
    val provider =
        providers.firstOrNull { candidate -> candidate.id == lane.providerId }
            ?: throw IllegalArgumentException(
                "no Fabric runtime provider supports Minecraft ${identity.gameVersion}: " +
                    "${lane.providerId}:provider-missing",
            )
    val support = provider.support(identity)
    if (support.state != FabricRuntimeSupportState.SUPPORTED) {
        throw IllegalArgumentException(
            "no Fabric runtime provider supports Minecraft ${identity.gameVersion}: " +
                "${provider.id}:${support.reason}",
        )
    }
    return FabricRuntimeProviderSelection(
        provider = provider,
        support = support,
        access = provider.createAccess(identity),
    )
}

private val machineCode = Regex("[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)*")
