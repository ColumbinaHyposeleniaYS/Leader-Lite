/*
 * Unfair port: attach the ViaLoadingBase pipeline at the tail of the
 * connect-time channel initializer (net.minecraft.network.NetworkManager$5).
 */

package com.viaversion.viaforge.mixin.impl.connect;

import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import de.florianmichael.viamcp.MCPVLBPipeline;
import de.florianmichael.viamcp.ViaMCP;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.NetworkManager$5")
public class MixinNetworkManager_5 {

    @Inject(method = "initChannel", at = @At(value = "TAIL"), remap = false)
    private void hookVLBPipeline(Channel channel, CallbackInfo ci) {
        if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() == ViaMCP.NATIVE_VERSION) {
            return;
        }

        final UserConnectionImpl user = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(user);
        channel.pipeline().addLast(new MCPVLBPipeline(user));
    }
}
