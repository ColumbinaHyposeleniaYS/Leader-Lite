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
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
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
     * Registers the swap-offhand binding BEFORE loadOptions() runs, so a
     * user rebind persisted in options.txt ("key_key.swapOffhand:..") is
     * applied on every start. The old RETURN injection ran after the file
     * had already been read and silently dropped saved rebinds.
     */
    @Inject(
            method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/GameSettings;loadOptions()V"),
            require = 0
    )
    private void viaforge$registerOffhandKeyBeforeLoad(Minecraft minecraft, File optionsFile, CallbackInfo ci) {
        viaforge$registerOffhandKey();
    }

    /**
     * Fallback: if the loadOptions() INVOKE point ever drifts, register at
     * constructor end (old behavior - binding works, saved rebinds are not
     * reloaded). Also refreshes the static key array hash after load either way.
     */
    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At("RETURN"))
    private void viaforge$registerOffhandKeyAtReturn(Minecraft minecraft, File optionsFile, CallbackInfo ci) {
        final boolean wasRegistered = viaforge$offhandKeyRegistered;
        viaforge$registerOffhandKey();
        if (wasRegistered || viaforge$offhandKeyRegistered) {
            KeyBinding.resetKeyBindingArrayAndHash();
        }
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
