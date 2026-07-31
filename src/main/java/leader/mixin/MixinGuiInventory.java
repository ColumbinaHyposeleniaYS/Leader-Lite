package leader.mixin;

import leader.Leader;
import leader.module.modules.player.InvManager;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiInventory.class)
public abstract class MixinGuiInventory {
    @Inject(method = {"drawScreen"}, at = {@At("HEAD")}, cancellable = true)
    private void hideLegitSpoofInventory(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (Leader.moduleManager == null) return;
        InvManager invManager = (InvManager) Leader.moduleManager.modules.get(InvManager.class);
        if (invManager != null && invManager.shouldHideLegitSpoofInventory()) {
            callbackInfo.cancel();
        }
    }
}
