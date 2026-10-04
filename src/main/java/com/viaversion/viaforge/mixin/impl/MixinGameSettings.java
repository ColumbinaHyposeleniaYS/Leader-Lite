/*
 * This file is part of ViaForge - https://github.com/ViaVersion/ViaForge
 * Copyright (C) 2021-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viaforge.mixin.impl;

import leader.util.via.ModernOffhandKeyBinding;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(GameSettings.class)
public class MixinGameSettings implements ModernOffhandKeyBinding {

    @Unique
    private KeyBinding viaforge$swapOffhandKey;

    @Unique
    private boolean viaforge$offhandKeyRegistered;

    @Override
    public KeyBinding getSwapOffhandKey() {
        return viaforge$swapOffhandKey;
    }

    /**
     * Registers the swap-offhand binding at the HEAD of loadOptions(), which the
     * (Minecraft, File) constructor calls before reading options.txt. This keeps
     * the original goal (a saved "key.swapOffhand" rebind is applied on every
     * start) with a 0.7.11-safe injection point.
     *
     * NOTE: do NOT inject into the GameSettings <init> with @At("INVOKE").
     * Mixin 0.7.11's CallbackInjector.sanityCheck hard-fails at APPLY time for
     * any constructor target that is not RETURN ("Found injection point type
     * BeforeInvoke targetting a ctor ... Only RETURN allowed for a ctor target"),
     * which took the whole GameSettings class down (NoClassDefFoundError) and
     * crashed the game during startGame. The previous RETURN-on-ctor fallback is
     * also gone: it ran after loadOptions had already parsed options.txt, so
     * saved rebinds were silently dropped; loadOptions-HEAD strictly dominates it.
     */
    @Inject(method = "loadOptions", at = @At("HEAD"), require = 1)
    private void viaforge$registerOffhandKeyBeforeLoad(CallbackInfo ci) {
        viaforge$registerOffhandKey();
    }

    @Unique
    private void viaforge$registerOffhandKey() {
        if (viaforge$offhandKeyRegistered) {
            return;
        }
        viaforge$offhandKeyRegistered = true;
        viaforge$swapOffhandKey = new KeyBinding("key.swapOffhand", Keyboard.KEY_F, "key.categories.inventory");
        final GameSettings settings = (GameSettings) (Object) this;
        settings.keyBindings = Arrays.copyOf(settings.keyBindings, settings.keyBindings.length + 1);
        settings.keyBindings[settings.keyBindings.length - 1] = viaforge$swapOffhandKey;
        KeyBinding.resetKeyBindingArrayAndHash();
    }
}
