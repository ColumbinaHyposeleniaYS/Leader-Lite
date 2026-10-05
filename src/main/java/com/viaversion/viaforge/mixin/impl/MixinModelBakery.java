/*
 * Leader-Lite port: bakes the modern (ViaBackwards) item models so that
 * ItemModelMesher can resolve them at render time.
 * Ported from the Unfair client's inline patch of ModelBakery.loadItemModels.
 */
package com.viaversion.viaforge.mixin.impl;

import java.io.IOException;
import java.util.Map;

import leader.util.via.ViaBackwardsItemModels;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBakery.class)
public abstract class MixinModelBakery {

    @Shadow
    @Final
    private Map<ResourceLocation, ModelBlock> models;

    @Shadow
    @Final
    private Map<String, ResourceLocation> itemLocations;

    @Invoker("loadModel")
    protected abstract ModelBlock viaforge$loadModel(ResourceLocation location) throws IOException;

    /**
     * TAIL of loadItemModels: upstream inserts its extra loading loop right
     * here, i.e. BEFORE loadModels() runs its parent-resolution deque over
     * this.models - so the parents of the modern models get resolved by the
     * vanilla pass and the vanilla itemLocations bake loop bakes our entries
     * under "<name>#inventory", which MixinItemModelMesher then looks up.
     */
    @Inject(method = "loadItemModels", at = @At("TAIL"), require = 1)
    private void viaforge$loadViaModels(CallbackInfo ci) {
        int ok = 0;
        int fail = 0;

        for (String s : ViaBackwardsItemModels.getModelNames()) {
            ResourceLocation location = new ResourceLocation("minecraft", "item/" + s);

            if (this.models.containsKey(location)) {
                // Already loaded (registered ItemBlock etc.) - just make sure the
                // itemLocations entry exists so the bake loop covers it.
                this.itemLocations.put(s, location);
                ViaBackwardsItemModels.markBakedViaModel(s);
                ok++;
                continue;
            }

            try {
                ModelBlock modelblock = this.viaforge$loadModel(location);
                this.models.put(location, modelblock);
                this.itemLocations.put(s, location);
                ViaBackwardsItemModels.markBakedViaModel(s);
                ok++;
            } catch (Exception ignored) {
                fail++;
            }
        }

        System.out.println("[Leader-Lite] Via item models loaded: " + ok + " ok, " + fail + " failed");
    }
}
