package com.minekube.craftless.driver.runtime

import com.minekube.craftless.bridge.hmc.ClientAction
import com.minekube.craftless.bridge.hmc.HmcBridgeBackend
import com.minekube.craftless.driver.api.ConnectionTarget
import com.minekube.craftless.driver.api.DriverRuntimeMetadata

class HmcBridgeDriverBackend(
    private val bridge: HmcBridgeBackend,
) : DriverBackend {
    override fun connect(
        clientId: String,
        target: ConnectionTarget,
    ): DriverBackendResult {
        val result = bridge.connect(clientId, "${target.host}:${target.port}")
        require(result.action == ClientAction.CONNECT) { "driver backend returned ${result.action} for connect" }
        return DriverBackendResult(DriverBackendAction.CONNECT, result.publicDescription)
    }

    override fun stop(clientId: String): DriverBackendResult {
        val result = bridge.stop(clientId)
        require(result.action == ClientAction.STOP) { "driver backend returned ${result.action} for stop" }
        return DriverBackendResult(DriverBackendAction.STOP, result.publicDescription)
    }

    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata =
        DriverRuntimeMetadata(
            driver = "craftless-driver-bridge",
            permissionsFingerprint = "bridge-evidence",
        )
}
