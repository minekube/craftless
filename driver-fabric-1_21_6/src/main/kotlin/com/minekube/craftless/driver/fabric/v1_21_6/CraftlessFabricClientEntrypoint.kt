package com.minekube.craftless.driver.fabric.v1_21_6

import net.fabricmc.api.ClientModInitializer

class CraftlessFabricClientEntrypoint : ClientModInitializer {
    override fun onInitializeClient() {
        FabricDriverBackend.install(FabricDriverBackend.real())
    }
}
