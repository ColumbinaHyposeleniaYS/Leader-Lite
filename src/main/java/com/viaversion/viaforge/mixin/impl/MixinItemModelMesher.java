/*
 * Leader-Lite port: model resolution for modern (ViaBackwards) items.
 * Ported from the Unfair client's inline patch of ItemModelMesher.getItemModel(ItemStack).
 */
package com.viaversion.viaforge.mixin.impl;

import leader.util.via.LegacyHandBakedModel;
import leader.util.via.ViaBackwardsItemModels;
import net.minecraft.block.Block;
import net.minecraft.block.ModernBlock;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemModelMesher.class)
public abstract class MixinItemModelMesher {

    @Shadow
    @Final
    private ModelManager modelManager;

    /**
     * Runs after the vanilla resolution. If the stack carries a modern
     * (ViaBackwards) identity whose model got baked by MixinModelBakery, the
     * returned model is swapped for the modern one; otherwise the vanilla
     * model is passed through LegacyHandBakedModel.wrap so modern display
     * transforms do not corrupt the 1.8 hand/GUI rendering.
     */
    @Inject(method = "getItemModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/resources/model/IBakedModel;",
            at = @At("RETURN"), require = 1, cancellable = true)
    private void viaforge$viaModel(ItemStack stack, CallbackInfoReturnable<IBakedModel> cir) {
        if (stack == null || cir.getReturnValue() == null) {
            return;
        }

        String viaModelName = ViaBackwardsItemModels.getModelName(stack);
        String itemModelName = viaModelName == null
                || "elytra".equals(viaModelName) || "elytra_broken".equals(viaModelName)
                ? null : viaModelName;

        IBakedModel resolved = null;
        if (itemModelName != null && ViaBackwardsItemModels.isBakedViaModel(itemModelName)) {
            resolved = this.modelManager.getModel(new ModelResourceLocation(itemModelName, "inventory"));
        }

        if (resolved == null) {
            resolved = cir.getReturnValue();
        }

        IBakedModel wrapped = LegacyHandBakedModel.wrap(resolved, viaModelName, isModernBlockItem(stack, itemModelName));
        if (wrapped != resolved) {
            cir.setReturnValue(wrapped);
        }
    }

    private static boolean isModernBlockItem(ItemStack stack, String modelName) {
        if (ViaBackwardsItemModels.isBlockModel(modelName)) {
            return true;
        }

        Block block = modelName == null ? null : Block.blockRegistry.getObject(new ResourceLocation(modelName));
        if (!(block instanceof ModernBlock) && stack != null && stack.getItem() instanceof ItemBlock) {
            block = ((ItemBlock) stack.getItem()).getBlock();
        }

        return block instanceof ModernBlock;
    }
}
