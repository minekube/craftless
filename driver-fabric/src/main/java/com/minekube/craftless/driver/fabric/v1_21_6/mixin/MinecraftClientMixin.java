package com.minekube.craftless.driver.fabric.v1_21_6.mixin;

import com.minekube.craftless.driver.fabric.v1_21_6.FabricEventHooks;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void craftless$recordClientTick(CallbackInfo info) {
        FabricEventHooks.recordClientTick();
    }
}
