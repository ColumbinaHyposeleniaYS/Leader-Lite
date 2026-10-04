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

import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.Protocol1_20_3To1_20_2;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundConfigurationPackets1_20_2;
import de.florianmichael.viamcp.ViaMCP;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import leader.util.via.ModernOffhandInteraction;
import leader.util.via.ModernOffhandKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public EntityPlayerSP thePlayer;

    @Shadow
    public MovingObjectPosition objectMouseOver;

    @Shadow
    public PlayerControllerMP playerController;

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    public GuiScreen currentScreen;

    @Unique
    private boolean viaforge$delayedAttackSwing;

    /**
     * Unfair: during the 1.20.2+ configuration phase, reply with brand and
     * client information once ViaMCP stored the user connection.
     */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void viaforge$sendConfigurationPackets(CallbackInfo ci) {
        if (ViaMCP.INSTANCE == null || ViaMCP.INSTANCE.user == null) {
            return;
        }

        try {
            PacketWrapper packetBrand = PacketWrapper.create(
                    ServerboundConfigurationPackets1_20_2.CUSTOM_PAYLOAD, ViaMCP.INSTANCE.user);
            packetBrand.write(Types.STRING, "minecraft:brand");
            packetBrand.write(Types.STRING, "vanilla");
            packetBrand.sendToServer(Protocol1_20_3To1_20_2.class);

            packetBrand = PacketWrapper.create(
                    ServerboundConfigurationPackets1_20_2.CLIENT_INFORMATION, ViaMCP.INSTANCE.user);
            packetBrand.write(Types.STRING, this.gameSettings.language.toLowerCase());
            packetBrand.write(Types.BYTE, (byte) this.gameSettings.renderDistanceChunks);
            packetBrand.write(Types.VAR_INT, this.gameSettings.chatVisibility.ordinal());
            packetBrand.write(Types.BOOLEAN, this.gameSettings.chatColours);

            int mask = 0;
            for (EnumPlayerModelParts part : this.gameSettings.getModelParts()) {
                mask |= part.getPartMask();
            }

            packetBrand.write(Types.UNSIGNED_BYTE, (short) mask);
            packetBrand.write(Types.VAR_INT, 1);
            packetBrand.write(Types.BOOLEAN, true);
            packetBrand.write(Types.BOOLEAN, true);
            packetBrand.sendToServer(Protocol1_20_3To1_20_2.class);
        } catch (Exception exception) {
            System.out.println("ViaVersion packet transformation failed (expected during connection setup): "
                    + exception.getMessage());
        } finally {
            ViaMCP.INSTANCE.user = null;
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), require = 0)
    private void viaforge$beginModernRightClick(CallbackInfo ci) {
        if (viaforge$isModernTarget()) {
            ModernOffhandInteraction.beginRightClick();
        }
    }

    @Redirect(
            method = "rightClickMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingItem()V"
            ),
            require = 0
    )
    private void viaforge$swingCorrectHandAfterBlockUse(EntityPlayerSP player) {
        if (!viaforge$isModernTarget() || !ModernOffhandInteraction.wasClientOffhandAction()) {
            player.swingItem();
        }
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireKeyInput()V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void viaforge$handleOffhandSwapAfterKeyInput(CallbackInfo ci) {
        if (!viaforge$isModernTarget()
                || thePlayer == null
                || playerController == null
                || currentScreen != null
                || !(gameSettings instanceof ModernOffhandKeyBinding)) {
            return;
        }

        final ModernOffhandKeyBinding keys = (ModernOffhandKeyBinding) gameSettings;
        if (keys.getSwapOffhandKey() != null
                && keys.getSwapOffhandKey().isPressed()) {
            ModernOffhandInteraction.sendSwapItemWithOffhand(thePlayer);
        }
    }

    @Inject(method = "rightClickMouse", at = @At("RETURN"), require = 0)
    private void viaforge$rightClickOffhandAir(CallbackInfo ci) {
        if (!viaforge$isModernTarget()
                || thePlayer == null
                || playerController == null
                || thePlayer.inventory.getCurrentItem() != null
                || !ModernOffhandInteraction.hasOffhand(thePlayer)) {
            return;
        }

        if (objectMouseOver == null
                || objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            ModernOffhandInteraction.sendUseItem(thePlayer);
        }
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), require = 0)
    private void viaforge$resetDelayedAttackSwing(CallbackInfo ci) {
        viaforge$delayedAttackSwing = false;
    }

    /** Modern clients perform the selected attack/dig action before swinging. */
    @Redirect(
            method = "clickMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;swingItem()V"
            ),
            require = 0
    )
    private void viaforge$delayModernAttackSwing(EntityPlayerSP player) {
        if (viaforge$isModernTarget()) {
            // Entity attacks send their animation from PlayerControllerMP,
            // immediately after ATTACK and before local sprint/attack slowing.
            viaforge$delayedAttackSwing = objectMouseOver == null
                    || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY;
        } else {
            player.swingItem();
        }
    }

    @Inject(method = "clickMouse", at = @At("RETURN"), require = 0)
    private void viaforge$sendModernAttackSwing(CallbackInfo ci) {
        if (viaforge$isModernTarget() && viaforge$delayedAttackSwing && thePlayer != null) {
            thePlayer.swingItem();
        }
    }

    private static boolean viaforge$isModernTarget() {
        return leader.util.via.ModernOffhandInteraction.isModernTarget();
    }

}
