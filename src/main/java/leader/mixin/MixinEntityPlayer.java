package leader.mixin;

import leader.Leader;
import leader.module.modules.player.KeepSprint;
import leader.util.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityPlayer.class}, priority = 9999)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @Unique
    private boolean leader$restoreLegitSprint;

    @Inject(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At("HEAD")
    )
    private void prepareLegitKeepSprint(Entity target, CallbackInfo callbackInfo) {
        this.leader$restoreLegitSprint = PlayerUtil.prepareLegitKeepSprintAttack();
    }

    @Inject(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At("RETURN")
    )
    private void restoreLegitKeepSprint(Entity target, CallbackInfo callbackInfo) {
        PlayerUtil.restoreLegitKeepSprintAttack(this.leader$restoreLegitSprint);
        this.leader$restoreLegitSprint = false;
    }

    @ModifyConstant(
            method = {"attackTargetEntityWithCurrentItem"},
            constant = @Constant(doubleValue = 0.6D)
    )
    private double modifyVanillaKeepSprintSlowdown(double slowdown) {
        if (Leader.moduleManager == null) {
            return slowdown;
        }

        KeepSprint keepSprint = (KeepSprint) Leader.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint == null || !keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
            return slowdown;
        }

        return slowdown + (1.0D - slowdown)
                * (1.0D - keepSprint.slowdown.getValue().doubleValue() / 100.0D);
    }

    @Redirect(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"
            )
    )
    private void keepVanillaSprint(EntityPlayer entityPlayer, boolean sprinting) {
        if (Leader.moduleManager == null) {
            entityPlayer.setSprinting(sprinting);
            return;
        }

        KeepSprint keepSprint = (KeepSprint) Leader.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint != null && keepSprint.isEnabled() && keepSprint.shouldKeepSprint()) {
            entityPlayer.setSprinting(true);
            return;
        }

        entityPlayer.setSprinting(sprinting);
    }
}
