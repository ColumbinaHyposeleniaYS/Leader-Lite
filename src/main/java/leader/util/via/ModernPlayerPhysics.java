// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInput;

public interface ModernPlayerPhysics {

    boolean isModernSwimming();

    boolean wasModernSwimming();

    boolean isModernSubmergedInWater();

    void setModernSubmergedInWater(boolean submerged);

    boolean wasModernEyeInWater();

    float getModernEyeHeight();

    double getModernWaterHeight();

    void setModernWaterHeight(double height);

    double getModernLavaHeight();

    void setModernLavaHeight(double height);

    boolean isTouchingModernLava();

    void setTouchingModernLava(boolean touching);

    BlockPos getMainSupportingBlock();

    boolean wasSupportingBlockOnGround();

    void setMainSupportingBlock(BlockPos position, boolean onGround);

    void markLocalItemUseFinished();

    void confirmServerItemUseFinished();

    void updateModernMovementInput(MovementInput input);

    // Leader-Lite addition: kept from the previous ViaForge compat layer
    // (the Unfair source implements this inline in its Entity sources).
    boolean isMinorHorizontalCollision();

    void setMinorHorizontalCollision(boolean minor);
}
