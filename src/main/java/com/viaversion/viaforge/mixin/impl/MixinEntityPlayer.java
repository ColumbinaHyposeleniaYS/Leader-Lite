/*
 * This file is part of ViaForge - https://github.com/ViaVersion/ViaForge
 * Copyright (C) 2021-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.viaversion.viaforge.mixin.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import leader.util.via.ModernOffhandInteraction;
import leader.util.via.ModernOffhandInventory;
import leader.util.via.ModernPlayerPhysics;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    @Shadow
    private ItemStack itemInUse;

    @Shadow
    private int itemInUseCount;

    @Shadow
    protected abstract void updateItemUse(ItemStack stack, int particleCount);

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true, require = 0)
    private void viaforge$modernPoseEyeHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof EntityPlayerSP && viaforge$isModernTarget()) {
            cir.setReturnValue(((ModernPlayerPhysics) this).getModernEyeHeight());
        }
    }

    @Redirect(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"
            ),
            require = 0
    )
    private ItemStack viaforge$keepOffhandUseActive(InventoryPlayer inventory) {
        if ((Object) this instanceof EntityPlayerSP && viaforge$isModernTarget()) {
            final ItemStack offhand = ModernOffhandInteraction.getOffhand((EntityPlayer) (Object) this);
            if (itemInUse != null && itemInUse == offhand) {
                return offhand;
            }
        }
        return inventory.getCurrentItem();
    }

    @Inject(method = "onItemUseFinish", at = @At("HEAD"), cancellable = true, require = 0)
    private void viaforge$finishModernItemUse(CallbackInfo ci) {
        if (!((Object) this instanceof EntityPlayerSP) || !viaforge$isModernTarget()) {
            return;
        }

        ((ModernPlayerPhysics) this).markLocalItemUseFinished();
        final EntityPlayer player = (EntityPlayer) (Object) this;
        final ItemStack offhand = ModernOffhandInteraction.getOffhand(player);
        if (itemInUse == null || itemInUse != offhand) {
            return;
        }

        final ItemStack original = itemInUse;
        updateItemUse(original, 16);
        ItemStack result = original.onItemUseFinish(player.worldObj, player);
        result = ForgeEventFactory.onItemUseFinish(player, original, itemInUseCount, result);
        ((ModernOffhandInventory) player.inventory).setOffhand(
                result != null && result.stackSize > 0 ? result : null
        );
        player.clearItemInUse();
        ci.cancel();
    }

    @Inject(method = "handleStatusUpdate", at = @At("HEAD"), require = 0)
    private void confirmServerItemUseFinished(byte id, CallbackInfo ci) {
        if (id == 9
                && (Object) this instanceof EntityPlayerSP
                && viaforge$isModernTarget()) {
            ((ModernPlayerPhysics) this).confirmServerItemUseFinished();
        }
    }

    private static boolean viaforge$isModernTarget() {
        return leader.util.via.ModernOffhandInteraction.isModernTarget();
    }
}
