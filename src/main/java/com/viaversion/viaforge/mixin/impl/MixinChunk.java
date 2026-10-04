/*
 * Leader-Lite port: exposes the Unfair modern-height chunk helpers through a
 * mixin interface. Sections outside the 1.8 storage range (0-15) are ignored,
 * matching the previous ViaForge-based build behaviour.
 */

package com.viaversion.viaforge.mixin.impl;

import cn.unfair.util.via.ModernChunkAccess;
import cn.unfair.util.via.ModernWorldHeight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class MixinChunk implements ModernChunkAccess {

    @Shadow
    private ExtendedBlockStorage[] storageArrays;

    @Shadow
    public net.minecraft.world.World worldObj;

    @Invoker("generateHeightMap")
    public abstract void viaforge$generateHeightMap();

    @Override
    public void refreshHeightMap() {
        this.viaforge$generateHeightMap();
    }

    @Override
    public boolean isValidY(int y) {
        return ModernWorldHeight.isValidY(y);
    }

    @Unique
    private static final net.minecraft.block.Block viaforge$airBlock = net.minecraft.init.Blocks.air;

    @Override
    public void setExtendedSection(int sectionY, IBlockState[] states) {
        // 1.8 has 16 storage sections only; anything outside the vanilla
        // height range cannot be represented and is intentionally ignored.
        if (sectionY < 0 || sectionY >= this.storageArrays.length) {
            return;
        }
        if (states == null || states.length != 4096) {
            return;
        }

        ExtendedBlockStorage storage = this.storageArrays[sectionY];
        if (storage == null) {
            storage = new ExtendedBlockStorage(sectionY << 4, !worldObj.provider.getHasNoSky());
            this.storageArrays[sectionY] = storage;
        }

        for (int index = 0; index < states.length; index++) {
            IBlockState state = states[index];
            if (state == null || state.getBlock() == viaforge$airBlock) {
                continue;
            }
            storage.set(index & 15, index >> 8 & 15, index >> 4 & 15, state);
        }
    }
}
