package leader.module.modules.player;

import leader.event.EventTarget;
import leader.event.types.EventType;
import leader.events.AttackEvent;
import leader.events.LivingUpdateEvent;
import leader.events.PacketEvent;
import leader.events.TickEvent;
import leader.Leader;
import leader.module.Module;
import leader.module.modules.combat.KillAura;
import leader.property.properties.BooleanProperty;
import leader.property.properties.IntProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import leader.util.KeyBindUtil;
import leader.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MovingObjectPosition;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 3, new String[]{"Vanilla", "Legit", "Grim", "Delay"});

    public final BooleanProperty onHurt = new BooleanProperty("OnHurt", false, () -> mode.getValue() == 1);

    public final PercentProperty slowdown = new PercentProperty("Slowdown", 0, () -> mode.getValue() == 0);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", false, () -> mode.getValue() == 0);
    public final BooleanProperty reachOnly = new BooleanProperty("Reach Only", false, () -> mode.getValue() == 0);

    public final BooleanProperty autoFactor = new BooleanProperty("Auto Factor", true, () -> mode.getValue() == 2);
    public final PercentProperty offsetBudget = new PercentProperty("Offset Budget", 50, () -> mode.getValue() == 2 && autoFactor.getValue());
    public final PercentProperty factor = new PercentProperty("Factor", 65, () -> mode.getValue() == 2 && !autoFactor.getValue());
    public final BooleanProperty grimGroundOnly = new BooleanProperty("Ground Only", true, () -> mode.getValue() == 2);

    public final PercentProperty delaySlowdown = new PercentProperty("Delay Slowdown", 60, () -> mode.getValue() == 3);
    public final IntProperty delayMaxTicks = new IntProperty("Delay MaxTicks", 3, 1, 5, () -> mode.getValue() == 3);

    private int disSprintTicks = 0;
    private final Deque<Packet<?>> delayedPackets = new ConcurrentLinkedDeque<>();
    private boolean awaitingSwing = false;
    private int delayCounter = 0;

    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean shouldKeepSprint() {
        switch (mode.getValue()) {
            case 1:
                return false;
            case 2:
                if (grimGroundOnly.getValue() && !mc.thePlayer.onGround) return false;
                return true;
            case 3:
                return true;
            default:
                if (groundOnly.getValue() && !mc.thePlayer.onGround) return false;
                return !reachOnly.getValue() || mc.objectMouseOver != null
                        && mc.objectMouseOver.hitVec != null
                        && mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
        }
    }

    public boolean isAttackNoSlow() {
        return isEnabled() && (shouldKeepSprint() || mode.getValue() == 3);
    }

    public double getSlowFactor() {
        switch (mode.getValue()) {
            case 1:
                return 0.6;
            case 2:
                if (autoFactor.getValue()) {
                    double speed = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
                    if (speed <= 0.0) return 1.0;
                    double budget = 0.001 * offsetBudget.getValue() / 100.0;
                    return Math.min(1.0, Math.max(0.6, 0.6 + budget / speed));
                }
                return factor.getValue().doubleValue() / 100.0;
            case 3:
                return 1.0;
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
        if (!this.isEnabled() || mode.getValue() != 3) return;
        if (event.getType() != EventType.SEND) return;
        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02 = (C02PacketUseEntity) event.getPacket();
            if (c02.getAction() == C02PacketUseEntity.Action.ATTACK) {
                event.setCancelled(true);
                delayedPackets.offer(c02);
                awaitingSwing = true;
            }
        } else if (event.getPacket() instanceof C0APacketAnimation && awaitingSwing) {
            event.setCancelled(true);
            delayedPackets.offer(event.getPacket());
            awaitingSwing = false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mode.getValue() != 3) return;
        if (event.getType() != EventType.PRE) return;
        if (delayedPackets.isEmpty()) {
            delayCounter = 0;
            return;
        }
        delayCounter++;
        if (delayCounter > delayMaxTicks.getValue()) {
            delayedPackets.clear();
            awaitingSwing = false;
            delayCounter = 0;
            return;
        }
        C02PacketUseEntity c02 = null;
        for (Packet<?> p : delayedPackets) {
            if (p instanceof C02PacketUseEntity) {
                c02 = (C02PacketUseEntity) p;
                break;
            }
        }
        if (c02 == null) return;
        Entity target = c02.getEntityFromWorld(mc.theWorld);
        if (target == null) {
            delayedPackets.clear();
            awaitingSwing = false;
            delayCounter = 0;
            return;
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        boolean aiming = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit == target;
        if (!aiming) return;
        KillAura killAura = (KillAura) Leader.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.isPlayerBlocking()) return;
        double factor = 0.6 + 0.4 * (1.0 - delaySlowdown.getValue().doubleValue() / 100.0);
        mc.thePlayer.motionX *= factor;
        mc.thePlayer.motionZ *= factor;
        while (!delayedPackets.isEmpty()) {
            PacketUtil.sendPacketNoEvent(delayedPackets.poll());
        }
        delayCounter = 0;
    }

    @Override
    public void onEnabled() {
        disSprintTicks = 0;
        delayedPackets.clear();
        awaitingSwing = false;
    }

    @Override
    public void onDisabled() {
        while (!delayedPackets.isEmpty()) {
            PacketUtil.sendPacketNoEvent(delayedPackets.poll());
        }
        awaitingSwing = false;
        if (mode.getValue() == 1) {
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
        }
    }

    @Override
    public String[] getSuffix() {
        switch (mode.getValue()) {
            case 2:
                if (autoFactor.getValue()) {
                    return new String[]{"Grim", String.format("%.0f%%", getSlowFactor() * 100)};
                }
                return new String[]{"Grim", factor.getValue() + "%"};
            case 3:
                return new String[]{"Delay", String.format("%.0f%%", delaySlowdown.getValue())};
            default:
                return new String[]{this.mode.getModeString()};
        }
    }
}
