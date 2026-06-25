package com.minekube.craftless.driver.fabric.v1_21_6

import net.fabricmc.api.ClientModInitializer

class CraftlessFabricClientEntrypoint : ClientModInitializer {
    override fun onInitializeClient() {
        val gateway = MinecraftFabricClientGateway()
        val backend = FabricDriverBackend.real(gateway)
        FabricDriverBackend.install(backend)
        FabricClientSmokeController.fromEnvironment().start(backend, gateway)
    }
}
