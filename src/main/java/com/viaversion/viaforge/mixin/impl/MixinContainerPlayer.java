/*
 * This file is part of ViaForge - https://github.com/ViaVersion/ViaForge
 */
package com.viaversion.viaforge.mixin.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerPlayer.class)
public class MixinContainerPlayer {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void viaforge$addOffhandSlot(
            InventoryPlayer inventory,
            boolean localWorld,
            EntityPlayer player,
            CallbackInfo ci
    ) {
        if (leader.util.via.ModernOffhandInteraction.isModernTarget()
                && player.worldObj.isRemote) {
            ((ContainerAccessor) this).viaforge$addSlotToContainer(
                    new Slot(inventory, 45, 77, 62)
            );
        }
    }
}
