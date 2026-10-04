package leader.module.modules.movement;

import leader.event.EventTarget;
import leader.events.TickEvent;
import leader.mixin.IAccessorEntityLivingBase;
import leader.module.Module;
import cn.unfair.util.via.ViaProtocol;
import leader.util.KeyBindUtil;
import leader.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public class Sprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasSprinting = false;
    public final BooleanProperty foxFix = new BooleanProperty("fov-fix", true);

    public Sprint() {
        super("Sprint", true, true);
    }

    public boolean shouldApplyFovFix(IAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        } else {
            AttributeModifier attributeModifier = ((IAccessorEntityLivingBase) mc.thePlayer).getSprintingSpeedBoostModifier();
            return attribute.getModifier(attributeModifier.getID()) == null && this.wasSprinting;
        }
    }

    public boolean shouldKeepFov(boolean boolean2) {
        return this.foxFix.getValue() && !boolean2 && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (mc.thePlayer == null || mc.theWorld == null) {
                this.wasSprinting = false;
                KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
                return;
            }
            switch (event.getType()) {
                case PRE:
                    // 1.9+: sneaking blocks sprint; 1.13+: sprinting is cancelled in water
                    if ((ViaProtocol.newerThanOrEqualTo1_9() && mc.gameSettings.keyBindSneak.isKeyDown())
                            || (ViaProtocol.newerThanOrEqualTo1_13() && mc.thePlayer.isInWater())) {
                        this.wasSprinting = false;
                        mc.thePlayer.setSprinting(false);
                        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
                        break;
                    }
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                    break;
                case POST:
                    this.wasSprinting = mc.thePlayer.isSprinting();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
    }
}
