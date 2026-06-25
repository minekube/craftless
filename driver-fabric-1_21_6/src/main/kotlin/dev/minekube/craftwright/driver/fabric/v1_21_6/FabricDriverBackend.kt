package dev.minekube.craftwright.driver.fabric.v1_21_6

import dev.minekube.craftwright.driver.api.ChatCommand
import dev.minekube.craftwright.driver.api.ConnectionTarget
import dev.minekube.craftwright.driver.runtime.DriverBackend
import dev.minekube.craftwright.driver.runtime.DriverBackendAction
import dev.minekube.craftwright.driver.runtime.DriverBackendResult

class FabricDriverBackend private constructor(
    private val mode: Mode,
) : DriverBackend {
    private val events = mutableListOf<String>()

    override fun connect(clientId: String, target: ConnectionTarget): DriverBackendResult {
        require(target.host.isNotBlank()) { "connection host is required" }
        require(target.port in 1..65535) { "connection port must be between 1 and 65535" }
        record("connect $clientId ${target.host}:${target.port}")
        return DriverBackendResult(DriverBackendAction.CONNECT, "fabric ${mode.id} connect requested")
    }

    override fun sendChat(clientId: String, command: ChatCommand): DriverBackendResult {
        require(command.message.isNotBlank()) { "chat message is required" }
        record("chat $clientId ${command.message}")
        return DriverBackendResult(DriverBackendAction.CHAT, command.message)
    }

    override fun stop(clientId: String): DriverBackendResult {
        record("stop $clientId")
        return DriverBackendResult(DriverBackendAction.STOP, "fabric ${mode.id} stop requested")
    }

    fun events(): List<String> = events.toList()

    private fun record(event: String) {
        events += event
    }

    private enum class Mode(val id: String) {
        PLACEHOLDER("placeholder"),
    }

    companion object {
        @Volatile
        private var installed: FabricDriverBackend? = null

        fun placeholder(): FabricDriverBackend = FabricDriverBackend(Mode.PLACEHOLDER)

        fun install(backend: FabricDriverBackend) {
            installed = backend
        }

        fun current(): FabricDriverBackend =
            installed ?: placeholder().also(::install)
    }
}
