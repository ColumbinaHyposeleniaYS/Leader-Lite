/*
 * Unfair port: show the target protocol version on the F3 debug screen.
 */

package com.viaversion.viaforge.mixin.impl;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOverlayDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {

    @Inject(method = "getDebugInfoRight", at = @At(value = "TAIL"))
    public void addViaForgeVersion(CallbackInfoReturnable<List<String>> cir) {
        final ProtocolVersion version = ViaLoadingBase.getInstance().getTargetVersion();

        if (version != ProtocolVersion.v1_8 && !Minecraft.getMinecraft().isSingleplayer()) {
            cir.getReturnValue().add("");
            cir.getReturnValue().add("Via: " + version.getName());
        }
    }

}
