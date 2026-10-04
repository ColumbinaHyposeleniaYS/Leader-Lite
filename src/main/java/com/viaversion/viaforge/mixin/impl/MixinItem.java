/*
 * Leader-Lite port: registers ItemBlocks for the modern blocks, mirroring the
 * Unfair source version of Item.registerItems.
 */

package com.viaversion.viaforge.mixin.impl;

import leader.util.via.ModernBlocks;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the tail of Item.registerItems to register ItemBlocks for the modern
 * blocks (see {@link ModernBlocks#registerItemBlocks()}).
 *
 * NOTE: do NOT reach Item.registerItemBlock with a static @Invoker or @Shadow
 * here. On Mixin 0.7.11 (JAVA_8 compat level) static accessors/invokers are
 * turned into renamed synthetic proxies ("Renaming @Unique method ..." log
 * line) while intra-mixin call sites are NOT rewritten, leaving a dangling
 * reference -> NoSuchMethodError at runtime (and a native @Invoker produces
 * a native-with-Code woven method -> ClassFormatError at class definition).
 * The registration therefore goes through ModernBlocks.registerItemBlocks(),
 * which resolves the private static method reflectively.
 */
@Mixin(Item.class)
public abstract class MixinItem {

    @Inject(method = "registerItems", at = @At("TAIL"))
    private static void viaforge$registerModernItems(CallbackInfo ci) {
        ModernBlocks.registerItemBlocks();
    }
}
