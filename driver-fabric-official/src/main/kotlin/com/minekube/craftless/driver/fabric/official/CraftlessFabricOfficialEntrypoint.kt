package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.driver.fabric.attach.FabricDriverSelfAttach
import com.minekube.craftless.driver.runtime.BackendDriverSession
import net.fabricmc.api.ClientModInitializer

class CraftlessFabricOfficialEntrypoint : ClientModInitializer {
    override fun onInitializeClient() {
        MinecraftOfficialFabricEventSources.register()
        val backend = OfficialFabricDriverBackend()
        FabricDriverSelfAttach.startFromEnvironment(
            sessionFactory = { clientId ->
                BackendDriverSession(clientId = clientId, backend = backend)
            },
        )
    }
}
