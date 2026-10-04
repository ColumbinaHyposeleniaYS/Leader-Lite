/*
 * Unfair port: host the AsyncVersionSlider protocol selector on the
 * multiplayer screen instead of the old ViaForge button.
 */

package com.viaversion.viaforge.mixin.impl;

import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends GuiScreen {

    @Inject(method = "initGui", at = @At("RETURN"))
    public void hookVersionSlider(CallbackInfo ci) {
        this.buttonList.add(ViaMCP.create().getOrCreateAsyncVersionSlider());
    }
}
