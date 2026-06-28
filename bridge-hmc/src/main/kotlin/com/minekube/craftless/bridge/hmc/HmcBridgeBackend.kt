package com.minekube.craftless.bridge.hmc

class HmcBridgeBackend private constructor(
    private val runner: BridgeCommandRunner,
) {
    fun connect(
        clientId: String,
        server: String,
    ): BridgeActionResult = run(ClientAction.CONNECT, clientId, "connect to $server", BridgeCommand("connect $server"))

    fun stop(clientId: String): BridgeActionResult = run(ClientAction.STOP, clientId, "stop client", BridgeCommand("stop"))

    private fun run(
        action: ClientAction,
        clientId: String,
        publicDescription: String,
        command: BridgeCommand,
    ): BridgeActionResult {
        require(clientId.isNotBlank()) { "client id is required" }
        val execution = runner.run(command)
        return BridgeActionResult(
            action = action,
            publicDescription = publicDescription,
            internalCommand = execution.command,
        )
    }

    companion object {
        fun dryRun(): HmcBridgeBackend = HmcBridgeBackend(BridgeCommandRunner { command -> BridgeCommandExecution(command) })
    }
}

fun interface BridgeCommandRunner {
    fun run(command: BridgeCommand): BridgeCommandExecution
}

data class BridgeCommandExecution(
    val command: BridgeCommand,
)

data class BridgeActionResult(
    val action: ClientAction,
    val publicDescription: String,
    val internalCommand: BridgeCommand,
)

class BridgeCommand internal constructor(
    private val value: String,
) {
    fun redacted(): String = REDACTED

    internal fun raw(): String = value

    companion object {
        private const val REDACTED = "<internal bridge command>"
    }
}

enum class ClientAction {
    CONNECT,
    STOP,
}
