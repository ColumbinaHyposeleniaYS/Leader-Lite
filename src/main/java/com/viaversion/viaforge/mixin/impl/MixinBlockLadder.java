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
import net.minecraft.block.BlockLadder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockLadder.class)
public abstract class MixinBlockLadder {

    @ModifyConstant(
            method = "setBlockBoundsBasedOnState",
            constant = @Constant(floatValue = 0.125F),
            require = 0
    )
    private float viaforge$modernLadderThickness(float original) {
        return viaforge$isModernTarget() ? 0.1875F : original;
    }

    private static boolean viaforge$isModernTarget() {
        return leader.util.via.ModernOffhandInteraction.isModernTarget();
    }

}
