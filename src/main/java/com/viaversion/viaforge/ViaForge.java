/*
 * Replaced by the Unfair ViaLoadingBase/ViaMCP stack (ViaVersion 5.11.0).
 * Kept as the Forge entry point; the old ViaForge platform is gone.
 */

package com.viaversion.viaforge;

import de.florianmichael.viamcp.ViaMCP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "viaforge", name = "ViaForge", acceptableRemoteVersions = "*", clientSideOnly = true, useMetadata = true)
public class ViaForge {

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        // Boot the ViaLoadingBase pipeline exactly like the Unfair client does.
        ViaMCP viaMCP = ViaMCP.create();
        viaMCP.getOrCreateAsyncVersionSlider().setVersion(ViaMCP.NATIVE_VERSION);
    }
}
