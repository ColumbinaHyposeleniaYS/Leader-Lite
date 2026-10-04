package cn.unfair.util.via;

import net.minecraft.block.state.IBlockState;

/**
 * Leader-Lite port helper: Unfair adds modern-height chunk support directly
 * to the Chunk sources. Sections outside the 1.8 storage range are ignored
 * (matching the behaviour of the previous ViaForge-based build).
 */
public interface ModernChunkAccess {
    void refreshHeightMap();

    void setExtendedSection(int sectionY, IBlockState[] states);

    boolean isValidY(int y);
}
