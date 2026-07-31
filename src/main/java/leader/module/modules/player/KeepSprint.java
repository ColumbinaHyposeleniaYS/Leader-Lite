package leader.module.modules.player;

import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Vanilla", "Legit"});
    public final PercentProperty slowdown = new PercentProperty("Slowdown", 0, () -> mode.getValue() == 0);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", false, () -> mode.getValue() == 0);
    public final BooleanProperty reachOnly = new BooleanProperty("Reach Only", false, () -> mode.getValue() == 0);

    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean isLegitMode() {
        return this.mode.getValue() == 1;
    }

    public boolean shouldKeepSprint() {
        if (this.isLegitMode()) {
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

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
