/*
 * Leader-Lite port: exposes the Unfair WorldClient prediction handler and
 * block state sync through a mixin interface.
 */

package com.viaversion.viaforge.mixin.impl;

import leader.util.via.BlockStatePredictionHandler;
import leader.util.via.ModernWorldClient;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient implements ModernWorldClient {

    @Unique
    private final BlockStatePredictionHandler viaforge$predictionHandler = new BlockStatePredictionHandler();

    @Override
    public BlockStatePredictionHandler predictionHandler() {
        return this.viaforge$predictionHandler;
    }

    @Override
    public void syncBlockState(BlockPos pos, IBlockState newState, Vec3 playerPos) {
        WorldClient self = (WorldClient) (Object) this;
        IBlockState oldState = self.getBlockState(pos);

        if (oldState.equals(newState)) {
            return;
        }

        self.setBlockState(pos, newState, 3);
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        if (player != null && player.worldObj == self) {
            AxisAlignedBB blockBB = newState.getBlock().getCollisionBoundingBox(self, pos, newState);

            if (blockBB != null) {
                blockBB = blockBB.offset(pos.getX(), pos.getY(), pos.getZ());
                AxisAlignedBB playerBB = player.getEntityBoundingBox();

                if (playerBB.intersectsWith(blockBB)) {
                    player.setPositionAndUpdate(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
                }
            }
        }
    }
}
