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
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad {

    @Inject(method = "getCollisionBoundingBox", at = @At("HEAD"), cancellable = true, require = 0)
    private void viaforge$modernCollisionShape(
            World world,
            BlockPos position,
            IBlockState state,
            CallbackInfoReturnable<AxisAlignedBB> cir
    ) {
        if (!viaforge$isModernTarget()) {
            return;
        }

        cir.setReturnValue(new AxisAlignedBB(
                position.getX() + 1.0D / 16.0D,
                position.getY(),
                position.getZ() + 1.0D / 16.0D,
                position.getX() + 15.0D / 16.0D,
                position.getY() + 1.5D / 16.0D,
                position.getZ() + 15.0D / 16.0D
        ));
    }

    private static boolean viaforge$isModernTarget() {
        return leader.util.via.ModernOffhandInteraction.isModernTarget();
    }
}
