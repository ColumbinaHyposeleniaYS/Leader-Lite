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
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    /** 1.8 adds a 0.1 collision border; modern hit tests use the real box. */
    @Redirect(
            method = "getMouseOver",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getCollisionBorderSize()F"
            ),
            require = 0
    )
    private float viaforge$modernCollisionBorder(Entity entity) {
        return viaforge$isModernTarget() ? 0.0F : entity.getCollisionBorderSize();
    }

    private static boolean viaforge$isModernTarget() {
        return cn.unfair.util.via.ModernOffhandInteraction.isModernTarget();
    }
}
