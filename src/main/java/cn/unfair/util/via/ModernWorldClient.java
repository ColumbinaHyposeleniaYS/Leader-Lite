package cn.unfair.util.via;

import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.block.state.IBlockState;

/**
 * Leader-Lite port helper: Unfair adds the block state prediction handler and
 * a sync method to WorldClient sources; here they are exposed through a mixin
 * interface so ViaMCP/BlockStatePredictionHandler keep working unchanged.
 */
public interface ModernWorldClient {
    BlockStatePredictionHandler predictionHandler();

    void syncBlockState(BlockPos pos, IBlockState newState, Vec3 playerPos);
}
