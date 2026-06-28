package com.minekube.craftless.driver.fabric

internal data class FabricDriverAttachEnvironment(
    val clientId: String,
    val daemonUrl: String,
) {
    companion object {
        const val CLIENT_ID: String = "CRAFTLESS_CLIENT_ID"
        const val DAEMON_URL: String = "CRAFTLESS_DAEMON_URL"

        fun from(env: Map<String, String> = System.getenv()): FabricDriverAttachEnvironment? {
            val clientId = env[CLIENT_ID]?.trim()?.takeIf { it.isNotBlank() } ?: return null
            val daemonUrl =
                env[DAEMON_URL]
                    ?.trim()
                    ?.trimEnd('/')
                    ?.takeIf { it.isNotBlank() }
                    ?: return null
            return FabricDriverAttachEnvironment(clientId = clientId, daemonUrl = daemonUrl)
        }
    }
}
