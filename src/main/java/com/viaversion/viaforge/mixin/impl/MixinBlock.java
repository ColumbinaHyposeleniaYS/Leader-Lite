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

    /**
     * Registers the modern (1.9-1.21) blocks at the tail of Block.registerBlocks.
     *
     * NOTE: this used to inject at @At("INVOKE", target =
     * "Lnet/minecraft/util/RegistryNamespaced;validateKey()V"). In Forge 1.8.9
     * Block.blockRegistry's declared type is RegistryNamespacedDefaultedByKey, so the
     * actual invokevirtual owner in registerBlocks is RegistryNamespacedDefaultedByKey
     * and the target NEVER matched. With @Inject's default require = 0 (confirmed in
     * Mixin 0.7.11 InjectionInfo: requiredCallbackCount defaults to 0), a zero-match
     * injection is silently skipped - the callback never ran and every modern block
     * lookup returned the registry default (air), which then made
     * Item.registerItemBlock fail for all 153 ItemBlocks.
     *
     * TAIL on the whole method is owner-agnostic and cannot drift this way; require = 1
     * turns any future regression into a loud, named error instead of silent no-op.
     */
    @Inject(method = "registerBlocks", at = @At("TAIL"), require = 1)
    private static void viaforge$registerModernBlocks(CallbackInfo ci) {
        ModernBlockRegistrar.registerAll();
    }
}
