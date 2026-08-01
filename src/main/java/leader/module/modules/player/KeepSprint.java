package leader.module.modules.player;

import leader.event.EventTarget;
import leader.event.types.EventType;
import leader.events.AttackEvent;
import leader.events.LivingUpdateEvent;
import leader.events.UpdateEvent;
import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import leader.util.KeyBindUtil;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Vanilla", "Legit"});
    public final BooleanProperty onHurt = new BooleanProperty("OnHurt", false, () -> mode.getValue() == 1);
    public final PercentProperty slowdown = new PercentProperty("Slowdown", 0, () -> mode.getValue() == 0);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", false, () -> mode.getValue() == 0);
    public final BooleanProperty reachOnly = new BooleanProperty("Reach Only", false, () -> mode.getValue() == 0);
    private int disSprintTicks = 0;

    public KeepSprint() {
        super("KeepSprint", false);
    }
    public boolean shouldKeepSprint() {
        if (this.mode.getValue() == 1) {
            return false;
        }

        if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        }
        return !this.reachOnly.getValue()
                || mc.objectMouseOver != null
                && mc.objectMouseOver.hitVec != null
                && mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
    }
    @EventTarget
    public void onAttack(AttackEvent event){
        if (this.isEnabled() && mode.getValue() == 1){
            this.disSprintTicks = 5;
        }
    }
    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event){
        if (this.isEnabled() && mode.getValue() == 1){
            if (disSprintTicks >= 0){
                if(onHurt.getValue() || mc.thePlayer.hurtTime == 0) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                    mc.thePlayer.setSprinting(false);
                }
                disSprintTicks--;
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
