// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: net.minecraft.client.renderer.ItemModelMesher$LegacyHandBakedModel
// (inner class) - extracted here so the mixin into ItemModelMesher can use it.
package leader.util.via;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

/**
 * Wraps a baked model whose JSON was authored for modern Minecraft with the
 * legacy 1.8 item transforms. Without this, modern flat items are drawn with
 * the modern "thirdperson_righthand" display block, which 1.8's hand renderer
 * interprets wrongly (items hover / clip through the hand).
 */
public final class LegacyHandBakedModel implements IBakedModel {
    private static final ItemTransformVec3f LEGACY_TOOL_THIRD_PERSON = transform(0.0F, 90.0F, -35.0F, 0.0F, 1.25F, -3.5F, 0.85F, 0.85F, 0.85F);
    private static final ItemTransformVec3f LEGACY_BOW_THIRD_PERSON = transform(5.0F, 80.0F, -45.0F, 0.75F, 0.0F, 0.25F, 1.0F, 1.0F, 1.0F);
    private static final ItemTransformVec3f LEGACY_FLAT_THIRD_PERSON = transform(-90.0F, 0.0F, 0.0F, 0.0F, 1.0F, -3.0F, 0.55F, 0.55F, 0.55F);
    private static final ItemTransformVec3f LEGACY_FIRST_PERSON = transform(0.0F, -135.0F, 25.0F, 0.0F, 4.0F, 2.0F, 1.7F, 1.7F, 1.7F);

    private final IBakedModel parent;
    private final ItemCameraTransforms transforms;

    private LegacyHandBakedModel(IBakedModel parent, ItemTransformVec3f thirdPerson, boolean resetGuiTransform) {
        this.parent = parent;
        ItemCameraTransforms original = parent.getItemCameraTransforms();
        this.transforms = new ItemCameraTransforms(
                thirdPerson == null ? original.thirdPerson : thirdPerson,
                thirdPerson == null ? original.firstPerson : LEGACY_FIRST_PERSON,
                original.head,
                resetGuiTransform ? ItemTransformVec3f.DEFAULT : original.gui,
                original.ground,
                original.fixed
        );
    }

    public static IBakedModel wrap(IBakedModel model, String viaModelName, boolean modernBlockItem) {
        ItemTransformVec3f thirdPerson = getLegacyThirdPersonTransform(model, viaModelName);
        boolean resetGuiTransform = modernBlockItem && model.isGui3d();
        return thirdPerson == null && !resetGuiTransform
                ? model
                : new LegacyHandBakedModel(model, thirdPerson, resetGuiTransform);
    }

    private static ItemTransformVec3f getLegacyThirdPersonTransform(IBakedModel model, String modelName) {
        if (modelName == null || modelName.equals("shield") || modelName.equals("shield_blocking") || modelName.equals("elytra") || modelName.equals("elytra_broken")) {
            return null;
        }

        if (modelName.equals("crossbow") || modelName.startsWith("crossbow_")) {
            return LEGACY_BOW_THIRD_PERSON;
        }

        if (modelName.endsWith("_sword") || modelName.endsWith("_pickaxe") || modelName.endsWith("_axe") || modelName.endsWith("_shovel") || modelName.endsWith("_hoe") || modelName.endsWith("_spear") || modelName.equals("trident")) {
            return LEGACY_TOOL_THIRD_PERSON;
        }

        if (model.isGui3d()) {
            return null;
        }

        return LEGACY_FLAT_THIRD_PERSON;
    }

    private static ItemTransformVec3f transform(float rotX, float rotY, float rotZ, float transX, float transY, float transZ, float scaleX, float scaleY, float scaleZ) {
        return new ItemTransformVec3f(new Vector3f(rotX, rotY, rotZ), new Vector3f(transX / 16.0F, transY / 16.0F, transZ / 16.0F), new Vector3f(scaleX, scaleY, scaleZ));
    }

    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing facing) {
        return this.parent.getFaceQuads(facing);
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return this.parent.getGeneralQuads();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.parent.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return this.parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.parent.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.transforms;
    }
}
