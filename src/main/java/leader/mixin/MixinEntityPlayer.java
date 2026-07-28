package leader.mixin;

import leader.Leader;
import leader.module.modules.misc.Disabler;
import leader.module.modules.player.KeepSprint;
import leader.util.PacketUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import static leader.config.Config.mc;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityPlayer.class}, priority = 9999)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @ModifyConstant(
            method = {"attackTargetEntityWithCurrentItem"},
            constant = {@Constant(
                    doubleValue = 0.6
            )}
    )
    private double attackTargetEntityWithCurrentItem(double speed) {
        if (Leader.moduleManager == null) {
            return speed;
        }
        KeepSprint keepSprint = (KeepSprint) Leader.moduleManager.modules.get(KeepSprint.class);
        if (!keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
            return speed;
        }
        if (keepSprint.mode.getValue() == 0) {
            return speed + (1.0 - speed) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0);
        }
        return speed;
    }

    @Redirect(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"
            )
    )
    private void setSprinnt(EntityPlayer entityPlayer, boolean boolean2) {
        if (Leader.moduleManager != null) {
            KeepSprint keepSprint = (KeepSprint) Leader.moduleManager.modules.get(KeepSprint.class);
            if (!keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
                entityPlayer.setSprinting(boolean2);
            } else {
                entityPlayer.setSprinting(true);
            }
        }
    }
}
