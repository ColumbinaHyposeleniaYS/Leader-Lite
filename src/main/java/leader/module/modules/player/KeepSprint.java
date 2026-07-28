package leader.module.modules.player;

import leader.event.EventTarget;
import leader.event.types.EventType;
import leader.events.AttackEvent;
import leader.events.PacketEvent;
import leader.events.UpdateEvent;
import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import leader.util.PacketUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C17PacketCustomPayload;

import java.util.Random;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Vanilla"});
    public final PercentProperty slowdown = new PercentProperty("Slowdown", 0, () -> mode.getValue() == 0);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", false, () -> mode.getValue() == 0);
    public final BooleanProperty reachOnly = new BooleanProperty("Reach Only", false, () -> mode.getValue() == 0);


    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean shouldKeepSprint() {
        if (mode.getValue() == 0) {
            if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
                return false;
            }
            return !this.reachOnly.getValue()
                    || mc.objectMouseOver != null
                    && mc.objectMouseOver.hitVec != null
                    && mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
        }
        return true;
    }
    @Override
    public String[] getSuffix() {
        if (mode.getValue() == 1) return new String[]{"Vanilla"};
        return new String[]{mode.getModeString()};
    }
}
