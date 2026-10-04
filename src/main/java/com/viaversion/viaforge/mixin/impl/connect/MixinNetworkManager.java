/*
 * Replaced by the Unfair ViaLoadingBase/ViaMCP stack (ViaVersion 5.11.0).
 * The old ViaForge platform (VersionTracker, ViaChannelInitializer, 1.6.4
 * tile encryption) is gone; only the VLB compression reorder and the
 * Unfair-style "ViaPacket" translation remain.
 */

package com.viaversion.viaforge.mixin.impl.connect;

import cn.unfair.util.via.ViaVersionFix;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.Protocol1_20_2To1_20;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.Protocol1_20To1_19_4;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viarewind.protocol.v1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ServerboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.vialoadingbase.netty.event.CompressionReorderEvent;
import java.util.Iterator;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.ViaPacket;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketSwapItemWithOffHand;
import net.minecraft.network.play.client.ServerBoundInteractAttack;
import net.minecraft.network.play.client.ServerBoundPlayerAction;
import net.minecraft.network.play.client.ServerBoundPlayerCommand;
import net.minecraft.network.play.client.ServerBoundSwing;
import net.minecraft.network.play.client.ServerBoundUseItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    @Unique
    private static final Logger viaforge$logger = LogManager.getLogger("ViaForge");

    /**
     * Unfair: setCompressionTreshold fires the VLB compression reorder event so the
     * via handlers stay on the correct side of the compression handlers.
     */
    @Inject(method = "setCompressionTreshold", at = @At("RETURN"), require = 0)
    private void viaforge$fireCompressionReorder(int threshold, CallbackInfo ci) {
        try {
            final UserConnection user = ViaVersionFix.connection();
            if (user != null && user.getChannel() != null) {
                user.getChannel().pipeline().fireUserEventTriggered(new CompressionReorderEvent());
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Unfair: intercept the custom "ViaPacket" classes and translate them manually,
     * because 1.8.9 has no serialization for them.
     */
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void viaforge$handleNewPackets(Packet packet, CallbackInfo ci) {
        if (viaforge$handleNewPacketsImpl(packet)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean viaforge$handleNewPacketsImpl(Packet packet) {
        if (!(packet instanceof ViaPacket)) {
            return false;
        }

        final boolean isHighVersion =
                ViaLoadingBase.getInstance().getTargetVersion().getVersion() > ProtocolVersion.v1_8.getVersion();

        if (!isHighVersion) {
            return true;
        }

        final Iterator<UserConnection> iterator =
                Via.getManager().getConnectionManager().getConnections().iterator();

        if (!iterator.hasNext()) {
            return true;
        }

        final UserConnection connection = iterator.next();

        try {
            if (packet instanceof CPacketPlayerTryUseItem) {
                PacketWrapper useItem = PacketWrapper.create(ServerboundPackets1_9.USE_ITEM, null, connection);
                useItem.write(Types.VAR_INT, ((CPacketPlayerTryUseItem) packet).getHand());
                useItem.sendToServer(Protocol1_9To1_8.class);
            } else if (packet instanceof CPacketSwapItemWithOffHand) {
                viaforge$sendSwapItemWithOffhand(connection);
            } else if (packet instanceof ServerBoundUseItem) {
                viaforge$sendUseItem(connection, (ServerBoundUseItem) packet);
            } else if (packet instanceof ServerBoundInteractAttack) {
                viaforge$sendInteractAttack(connection, (ServerBoundInteractAttack) packet);
            } else if (packet instanceof ServerBoundSwing) {
                viaforge$sendSwing(connection, (ServerBoundSwing) packet);
            } else if (packet instanceof ServerBoundPlayerAction) {
                final ServerBoundPlayerAction action = (ServerBoundPlayerAction) packet;
                PacketWrapper packetWrapper =
                        PacketWrapper.create(ServerboundPackets1_19.PLAYER_ACTION, connection);
                packetWrapper.write(Types.VAR_INT, action.getAction().ordinal());
                packetWrapper.write(Types.BLOCK_POSITION1_14, new BlockPosition(
                        action.getPos().getX(), action.getPos().getY(), action.getPos().getZ()));
                packetWrapper.write(Types.BYTE, (byte) action.getFacing().ordinal());
                packetWrapper.write(Types.VAR_INT,
                        action.getAction() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK
                                ? 0 : ViaVersionFix.sequence(connection));
                packetWrapper.sendToServer(Protocol1_19To1_18_2.class);
            } else if (packet instanceof ServerBoundPlayerCommand) {
                viaforge$sendPlayerCommand(connection, (ServerBoundPlayerCommand) packet);
            }
        } catch (Exception exception) {
            viaforge$logger.error("Failed to send ViaVersion packet", exception);
        }

        return true;
    }

    @Unique
    private static void viaforge$sendSwapItemWithOffhand(UserConnection connection) {
        final ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        if (target.newerThanOrEqualTo(ProtocolVersion.v1_21_2)) {
            PacketWrapper swap = PacketWrapper.create(ServerboundPackets1_21_2.PLAYER_ACTION, null, connection);
            swap.write(Types.VAR_INT, 6);
            swap.write(Types.BLOCK_POSITION1_14, new BlockPosition(0, 0, 0));
            swap.write(Types.VAR_INT, 0);
            swap.write(Types.VAR_INT, 0);
            swap.sendToServer(Protocol1_21_2To1_21.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            PacketWrapper swap = PacketWrapper.create(ServerboundPackets1_20_5.PLAYER_ACTION, null, connection);
            swap.write(Types.VAR_INT, 6);
            swap.write(Types.BLOCK_POSITION1_14, new BlockPosition(0, 0, 0));
            swap.write(Types.VAR_INT, 0);
            swap.write(Types.VAR_INT, 0);
            swap.sendToServer(Protocol1_20_5To1_20_3.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            PacketWrapper swap = PacketWrapper.create(ServerboundPackets1_20_2.PLAYER_ACTION, null, connection);
            swap.write(Types.VAR_INT, 6);
            swap.write(Types.BLOCK_POSITION1_14, new BlockPosition(0, 0, 0));
            swap.write(Types.VAR_INT, 0);
            swap.write(Types.VAR_INT, 0);
            swap.sendToServer(Protocol1_20_2To1_20.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_19_4)) {
            PacketWrapper swap = PacketWrapper.create(ServerboundPackets1_19_4.PLAYER_ACTION, null, connection);
            swap.write(Types.VAR_INT, 6);
            swap.write(Types.BLOCK_POSITION1_14, new BlockPosition(0, 0, 0));
            swap.write(Types.VAR_INT, 0);
            swap.write(Types.VAR_INT, 0);
            swap.sendToServer(Protocol1_20To1_19_4.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_19)) {
            PacketWrapper swap = PacketWrapper.create(ServerboundPackets1_19.PLAYER_ACTION, null, connection);
            swap.write(Types.VAR_INT, 6);
            swap.write(Types.BLOCK_POSITION1_14, new BlockPosition(0, 0, 0));
            swap.write(Types.VAR_INT, 0);
            swap.write(Types.VAR_INT, 0);
            swap.sendToServer(Protocol1_19To1_18_2.class);
        } else {
            PacketWrapper swap = PacketWrapper.create(ServerboundPackets1_9.PLAYER_ACTION, null, connection);
            swap.write(Types.VAR_INT, 6);
            swap.write(Types.BLOCK_POSITION1_8, new BlockPosition(0, 0, 0));
            swap.write(Types.BYTE, (byte) 0);
            swap.sendToServer(Protocol1_9To1_8.class);
        }
    }

    @Unique
    private static void viaforge$sendUseItem(UserConnection connection, ServerBoundUseItem useItem) {
        final ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            PacketWrapper use = PacketWrapper.create(ServerboundPackets1_20_5.USE_ITEM, null, connection);
            use.write(Types.VAR_INT, useItem.getHand().ordinal());
            use.write(Types.VAR_INT, ViaVersionFix.sequence(connection));
            use.sendToServer(Protocol1_20_5To1_20_3.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            PacketWrapper use = PacketWrapper.create(ServerboundPackets1_20_2.USE_ITEM, null, connection);
            use.write(Types.VAR_INT, useItem.getHand().ordinal());
            use.write(Types.VAR_INT, ViaVersionFix.sequence(connection));
            use.sendToServer(Protocol1_20_2To1_20.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_19)) {
            PacketWrapper use = PacketWrapper.create(ServerboundPackets1_19.USE_ITEM, null, connection);
            use.write(Types.VAR_INT, useItem.getHand().ordinal());
            use.write(Types.VAR_INT, ViaVersionFix.sequence(connection));
            use.sendToServer(Protocol1_19To1_18_2.class);
        } else {
            PacketWrapper use = PacketWrapper.create(ServerboundPackets1_9.USE_ITEM, null, connection);
            use.write(Types.VAR_INT, useItem.getHand().ordinal());
            use.sendToServer(Protocol1_9To1_8.class);
        }
    }

    @Unique
    private static void viaforge$sendInteractAttack(UserConnection connection, ServerBoundInteractAttack attack) {
        final ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        if (target.newerThanOrEqualTo(ProtocolVersion.v1_21_2)) {
            PacketWrapper interact = PacketWrapper.create(ServerboundPackets1_21_2.INTERACT, null, connection);
            interact.write(Types.VAR_INT, attack.getEntityId());
            interact.write(Types.VAR_INT, 1);
            interact.write(Types.BOOLEAN, false);
            interact.sendToServer(Protocol1_21_2To1_21.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            PacketWrapper interact = PacketWrapper.create(ServerboundPackets1_20_5.INTERACT, null, connection);
            interact.write(Types.VAR_INT, attack.getEntityId());
            interact.write(Types.VAR_INT, 1);
            interact.write(Types.BOOLEAN, false);
            interact.sendToServer(Protocol1_20_5To1_20_3.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            PacketWrapper interact = PacketWrapper.create(ServerboundPackets1_20_2.INTERACT, null, connection);
            interact.write(Types.VAR_INT, attack.getEntityId());
            interact.write(Types.VAR_INT, 1);
            interact.write(Types.BOOLEAN, false);
            interact.sendToServer(Protocol1_20_2To1_20.class);
        } else {
            PacketWrapper interact = PacketWrapper.create(ServerboundPackets1_19.INTERACT, null, connection);
            interact.write(Types.VAR_INT, attack.getEntityId());
            interact.write(Types.VAR_INT, 1);
            interact.write(Types.BOOLEAN, false);
            interact.sendToServer(Protocol1_19To1_18_2.class);
        }
    }

    @Unique
    private static void viaforge$sendSwing(UserConnection connection, ServerBoundSwing swing) {
        final ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        if (target.newerThanOrEqualTo(ProtocolVersion.v1_21_2)) {
            PacketWrapper swingPacket = PacketWrapper.create(ServerboundPackets1_21_2.SWING, null, connection);
            swingPacket.write(Types.VAR_INT, swing.getHand().ordinal());
            swingPacket.sendToServer(Protocol1_21_2To1_21.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            PacketWrapper swingPacket = PacketWrapper.create(ServerboundPackets1_20_5.SWING, null, connection);
            swingPacket.write(Types.VAR_INT, swing.getHand().ordinal());
            swingPacket.sendToServer(Protocol1_20_5To1_20_3.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            PacketWrapper swingPacket = PacketWrapper.create(ServerboundPackets1_20_2.SWING, null, connection);
            swingPacket.write(Types.VAR_INT, swing.getHand().ordinal());
            swingPacket.sendToServer(Protocol1_20_2To1_20.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_19)) {
            PacketWrapper swingPacket = PacketWrapper.create(ServerboundPackets1_19.SWING, null, connection);
            swingPacket.write(Types.VAR_INT, swing.getHand().ordinal());
            swingPacket.sendToServer(Protocol1_19To1_18_2.class);
        } else {
            PacketWrapper swingPacket = PacketWrapper.create(ServerboundPackets1_9.SWING, null, connection);
            swingPacket.write(Types.VAR_INT, swing.getHand().ordinal());
            swingPacket.sendToServer(Protocol1_9To1_8.class);
        }
    }

    @Unique
    private static void viaforge$sendPlayerCommand(UserConnection connection, ServerBoundPlayerCommand command) {
        final ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        if (target.newerThanOrEqualTo(ProtocolVersion.v1_21_2)) {
            PacketWrapper wrapper = PacketWrapper.create(ServerboundPackets1_21_2.PLAYER_COMMAND, null, connection);
            wrapper.write(Types.VAR_INT, command.getId());
            wrapper.write(Types.VAR_INT, command.getAction().ordinal());
            wrapper.write(Types.VAR_INT, command.getData());
            wrapper.sendToServer(Protocol1_21_2To1_21.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            PacketWrapper wrapper = PacketWrapper.create(ServerboundPackets1_20_5.PLAYER_COMMAND, null, connection);
            wrapper.write(Types.VAR_INT, command.getId());
            wrapper.write(Types.VAR_INT, command.getAction().ordinal());
            wrapper.write(Types.VAR_INT, command.getData());
            wrapper.sendToServer(Protocol1_20_5To1_20_3.class);
        } else if (target.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            PacketWrapper wrapper = PacketWrapper.create(ServerboundPackets1_20_2.PLAYER_COMMAND, null, connection);
            wrapper.write(Types.VAR_INT, command.getId());
            wrapper.write(Types.VAR_INT, command.getAction().ordinal());
            wrapper.write(Types.VAR_INT, command.getData());
            wrapper.sendToServer(Protocol1_20_2To1_20.class);
        } else {
            PacketWrapper wrapper = PacketWrapper.create(ServerboundPackets1_19.PLAYER_COMMAND, connection);
            wrapper.write(Types.VAR_INT, command.getId());
            wrapper.write(Types.VAR_INT, command.getAction().ordinal());
            wrapper.write(Types.VAR_INT, command.getData());
            wrapper.sendToServer(Protocol1_19To1_18_2.class);
        }
    }
}
