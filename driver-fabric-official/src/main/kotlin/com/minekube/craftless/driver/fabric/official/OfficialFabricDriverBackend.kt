package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.driver.api.ConnectionTarget
import com.minekube.craftless.driver.api.DriverActionInvocation
import com.minekube.craftless.driver.api.DriverActionResult
import com.minekube.craftless.driver.api.DriverActionStatus
import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import com.minekube.craftless.driver.runtime.DriverBackend
import com.minekube.craftless.driver.runtime.DriverBackendAction
import com.minekube.craftless.driver.runtime.DriverBackendResult
import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeCapabilityGraph
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSourceEvidence

internal class OfficialFabricDriverBackend : DriverBackend {
    override fun connect(
        clientId: String,
        target: ConnectionTarget,
    ): DriverBackendResult =
        DriverBackendResult(
            action = DriverBackendAction.CONNECT,
            message = "official lane metadata-only backend cannot connect $clientId to ${target.host}:${target.port}",
            observed = false,
        )

    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata =
        DriverRuntimeMetadata(
            loaderVersion = "0.19.3",
            driver = "craftless-driver-fabric-official",
            driverVersion = "0.1.0-SNAPSHOT",
            mappings = "craftless-official-bindings-26-2",
            installedModsFingerprint = "mods:official-lane-probe",
            registryFingerprint = "registries:unavailable",
            serverFeatureFingerprint = "server-features:unavailable",
            permissionsFingerprint = "permissions:local-client",
        )

    override fun runtimeGraph(clientId: String): RuntimeCapabilityGraph =
        RuntimeCapabilityGraph(
            clientId = clientId,
            resources =
                listOf(
                    RuntimeResourceNode(
                        id = "runtime",
                        availability = RuntimeAvailability.available(),
                        sourceEvidence =
                            listOf(
                                RuntimeSourceEvidence("runtime-lane", "latest-current-official"),
                                RuntimeSourceEvidence("runtime-status", "metadata-only"),
                                RuntimeSourceEvidence("runtime-java", "java:25"),
                            ),
                    ),
                ),
        )

    override fun invoke(
        clientId: String,
        invocation: DriverActionInvocation,
    ): DriverActionResult =
        DriverActionResult(
            action = invocation.action,
            status = DriverActionStatus.UNSUPPORTED,
            message = "official lane metadata-only backend has no generated runtime operation for ${invocation.action}",
        )

    override fun stop(clientId: String): DriverBackendResult =
        DriverBackendResult(
            action = DriverBackendAction.STOP,
            message = "stopped official lane metadata-only backend for $clientId",
        )
}
