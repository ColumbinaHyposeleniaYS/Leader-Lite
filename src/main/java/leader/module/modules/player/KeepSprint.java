package leader.module.modules.player;

import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("Mode",0,new String[]{"Vanilla","Strict"});
    public final PercentProperty slowdown = new PercentProperty("slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false,() -> mode.getValue() == 0);
    public final BooleanProperty reachOnly = new BooleanProperty("reach-only", false,() -> mode.getValue() == 0);

    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean shouldKeepSprint() {
        if (mode.getValue() == 0) {
            if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
                return false;
            } else {
                return !this.reachOnly.getValue() || mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
            }
        }
        else {
            return !mc.thePlayer.isSprinting();
        }
    }
    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}
