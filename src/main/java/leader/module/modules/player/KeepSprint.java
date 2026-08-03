package leader.module.modules.player;

import leader.event.EventTarget;
import leader.event.types.EventType;
import leader.events.AttackEvent;
import leader.events.LivingUpdateEvent;
import leader.events.PacketEvent;
import leader.events.TickEvent;
import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.IntProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import leader.util.KeyBindUtil;
import leader.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Vanilla", "Legit", "Blink"});

    public final BooleanProperty onHurt = new BooleanProperty("OnHurt", false, () -> mode.getValue() == 1);

    public final PercentProperty slowdown = new PercentProperty("Slowdown", 0, () -> mode.getValue() == 0);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", false, () -> mode.getValue() == 0);
    public final BooleanProperty reachOnly = new BooleanProperty("Reach Only", false, () -> mode.getValue() == 0);

    public final PercentProperty blinkSlowdown = new PercentProperty("BlinkSlowdown", 100, () -> mode.getValue() == 2);
    public final IntProperty blinkTicks = new IntProperty("BlinkTicks", 1, 1, 3, () -> mode.getValue() == 2);

    private int disSprintTicks = 0;

    private final Deque<DelayedAttack> delayedAttacks = new ConcurrentLinkedDeque<>();

    private static class DelayedAttack {
        final C02PacketUseEntity packet;
        int ticksRemaining;

        DelayedAttack(C02PacketUseEntity packet, int ticks) {
            this.packet = packet;
            this.ticksRemaining = ticks;
        }
    }

    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean shouldKeepSprint() {
        if (this.mode.getValue() == 1) {
            return false;
        }
        if (this.mode.getValue() == 2) {
            return true;
        }
        if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        }
        return !this.reachOnly.getValue()
                || mc.objectMouseOver != null
                && mc.objectMouseOver.hitVec != null
                && mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
    }

    public double getSlowFactor() {
        switch (mode.getValue()) {
            case 1: return 0.6;
            case 2: return 1.0;
            default:
                return 0.6 + 0.4 * (1.0 - slowdown.getValue().doubleValue() / 100.0);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled() && mode.getValue() == 1) {
            this.disSprintTicks = 3;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mode.getValue() == 1) {
            if (disSprintTicks >= 0) {
                if (onHurt.getValue() || mc.thePlayer.hurtTime == 0) {
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                    mc.thePlayer.setSprinting(false);
                }
                disSprintTicks--;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mode.getValue() != 2) return;
        if (event.getType() != EventType.SEND) return;

        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02 = (C02PacketUseEntity) event.getPacket();
            if (c02.getAction() == C02PacketUseEntity.Action.ATTACK) {
                event.setCancelled(true);
                delayedAttacks.offer(new DelayedAttack(c02, blinkTicks.getValue()));
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mode.getValue() != 2) return;
        if (event.getType() == EventType.PRE) {
            while (!delayedAttacks.isEmpty() && delayedAttacks.peek().ticksRemaining <= 0) {
                DelayedAttack da = delayedAttacks.poll();
                PacketUtil.sendPacketNoEvent(new C0APacketAnimation());
                PacketUtil.sendPacketNoEvent(da.packet);
                Entity target = da.packet.getEntityFromWorld(mc.theWorld);
                if (target != null) {
                    double factor = 0.6 + 0.4 * (1.0 - blinkSlowdown.getValue().doubleValue() / 100.0);
                    mc.thePlayer.motionX *= factor;
                    mc.thePlayer.motionZ *= factor;
                }
            }
        } else if (event.getType() == EventType.POST) {
            for (DelayedAttack da : delayedAttacks) {
                da.ticksRemaining--;
            }
        }
    }


    @Override
    public void onEnabled() {
        disSprintTicks = 0;
        clearDelayed();
    }

    @Override
    public void onDisabled() {
        while (!delayedAttacks.isEmpty()) {
            DelayedAttack da = delayedAttacks.poll();
            PacketUtil.sendPacketNoEvent(new C0APacketAnimation());
            PacketUtil.sendPacketNoEvent(da.packet);
        }
        if (mode.getValue() == 1) {
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
        }
    }

    private void clearDelayed() {
        delayedAttacks.clear();
    }

    @Override
    public String[] getSuffix() {
        if (mode.getValue() == 2) {
            return new String[]{"Blink", blinkTicks.getValue() + "t"};
        }
        return new String[]{this.mode.getModeString()};
    }
}
