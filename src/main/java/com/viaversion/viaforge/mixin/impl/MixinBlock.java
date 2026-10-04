/*
 * Leader-Lite port: hook for the modern block registration (Unfair port).
 */

package com.viaversion.viaforge.mixin.impl;

import net.minecraft.block.Block;
import net.minecraft.block.ModernBlockRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class MixinBlock {

    @Inject(
            method = "registerBlocks",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/RegistryNamespaced;validateKey()V",
                     shift = At.Shift.BEFORE)
    )
    private static void viaforge$registerModernBlocks(CallbackInfo ci) {
        ModernBlockRegistrar.registerAll();
    }
}
