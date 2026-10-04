package leader.module.modules.movement;

import leader.util.via.ViaProtocol;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import leader.event.EventTarget;
import leader.event.types.EventType;
import leader.events.PacketEvent;
import leader.events.StrafeEvent;
import leader.events.UpdateEvent;
import leader.module.Module;
import leader.util.ChatUtil;
import leader.util.KeyBindUtil;
import leader.util.MoveUtil;
import leader.util.PacketUtil;
import leader.property.properties.FloatProperty;
import leader.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Fly extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Vanilla", "Heypixel"});
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 1.0F, 0.0F, 100.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 1.0F, 0.0F, 100.0F);
    private final Deque<Packet<?>> heypixelPacketQueue = new ConcurrentLinkedDeque<>();
    private double verticalMotion = 0.0;
    private boolean heypixelDelay = false;
    private boolean heypixelEnabledWhileFlying = false;
    private boolean heypixelFlagged = false;

    public Fly() {
        super("Fly", false);
    }

    private void resetHeypixel() {
        this.heypixelDelay = false;
        this.heypixelFlagged = false;
        this.heypixelPacketQueue.clear();
    }

    private void flushHeypixelPackets() {
        Packet<?> packet;
        while ((packet = this.heypixelPacketQueue.poll()) != null) {
            PacketUtil.receivePacketNoEvent(packet);
        }
    }

    private void checkHeypixelAutoDisable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (this.heypixelFlagged) {
            return;
        }

        // Heypixel fly needs 1.16.5+ protocol features, auto-disable on lower versions
        if (ViaProtocol.olderThan(ProtocolVersion.v1_16_4)) {
            this.setEnabled(false);
            ChatUtil.dbg("&cHeypixel: server version is below 1.16.5, disabling Fly.");
            return;
        }

        if (!this.heypixelEnabledWhileFlying && !mc.thePlayer.capabilities.isFlying) {
            this.setEnabled(false);
            ChatUtil.dbg("&cHeypixel: player is not flying, disabling Fly.");
        }
    }

    private void handleHeypixelPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE || mc.thePlayer == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (mc.thePlayer.ticksExisted < 20) {
            this.heypixelPacketQueue.clear();
            return;
        }

        if (packet instanceof S08PacketPlayerPosLook) {
            if (mc.thePlayer.capabilities.isFlying && !this.heypixelDelay) {
                this.heypixelDelay = true;
            } else if (this.heypixelDelay) {
                if (!this.heypixelFlagged) {
                    this.heypixelFlagged = true;
                    ChatUtil.dbg("&cHeypixel: flagged, queued teleport to bypass BadPacketsN.");
                }
                this.heypixelPacketQueue.offer(packet);
            }
        }

        if (packet instanceof S39PacketPlayerAbilities) {
            S39PacketPlayerAbilities abilities = (S39PacketPlayerAbilities) packet;
            if (abilities.isFlying() && !this.heypixelDelay) {
                this.heypixelDelay = true;
            }
        }

        if (this.heypixelDelay && packet instanceof S32PacketConfirmTransaction) {
            this.heypixelPacketQueue.offer(packet);
            event.setCancelled(true);
        }
    }

    private void handleHeypixelSendPacket(PacketEvent event) {
        if (this.heypixelDelay && event.getPacket() instanceof C13PacketPlayerAbilities) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 0) {
            if (mc.thePlayer.posY % 1.0 != 0.0) {
                mc.thePlayer.motionY = this.verticalMotion;
            }
            MoveUtil.setSpeed(0.0);
            event.setFriction((float) MoveUtil.getBaseMoveSpeed() * this.hSpeed.getValue());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.mode.getValue() == 1) {
                this.checkHeypixelAutoDisable();
                return;
            }
            this.verticalMotion = 0.0;
            if (mc.currentScreen == null) {
                if (KeyBindUtil.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                    this.verticalMotion = this.verticalMotion + this.vSpeed.getValue().doubleValue() * 0.42F;
                }
                if (KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                    this.verticalMotion = this.verticalMotion - this.vSpeed.getValue().doubleValue() * 0.42F;
                }
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 1) {
            if (event.getType() == EventType.SEND) {
                this.handleHeypixelSendPacket(event);
            } else {
                this.handleHeypixelPacket(event);
            }
        }
    }

    @Override
    public void onEnabled() {
        this.resetHeypixel();
        this.heypixelEnabledWhileFlying = mc.thePlayer != null && mc.thePlayer.capabilities.isFlying;
        this.heypixelDelay = this.heypixelEnabledWhileFlying;
    }

    @Override
    public void onDisabled() {
        this.flushHeypixelPackets();
        this.resetHeypixel();
        if (mc.thePlayer != null) {
            mc.thePlayer.motionY = 0.0;
        }
        MoveUtil.setSpeed(0.0);
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
