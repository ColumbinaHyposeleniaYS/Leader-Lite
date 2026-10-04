/*
 * Leader-Lite port: registers ItemBlocks for the modern blocks, mirroring the
 * Unfair source version of Item.registerItems.
 */

package com.viaversion.viaforge.mixin.impl;

import leader.util.via.ModernBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.ModernBlock;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class MixinItem {

    // NOTE: must NOT be declared `native`! On Mixin 0.7.11 (JAVA_8 compat level) static
    // invokers become "static method proxies" copied into the target class with a
    // generated body; AccessorGenerator.createMethod only strips ACC_ABSTRACT and keeps
    // ACC_NATIVE, producing a native method with a Code attribute -> JVM ClassFormatError
    // ("Code attribute in native or abstract methods") when the target class is defined.
    // A dummy body is the correct declaration: Mixin replaces it with the generated
    // delegation to Item.registerItemBlock.
    @Invoker("registerItemBlock")
    private static void viaforge$invokeRegisterItemBlock(Block block) {
        throw new AssertionError();
    }

    @Inject(method = "registerItems", at = @At("TAIL"))
    private static void viaforge$registerModernItems(CallbackInfo ci) {
        viaforge$invokeRegisterItemBlock(ModernBlocks.dirt_path());
        viaforge$invokeRegisterItemBlock(ModernBlocks.campfire());
        viaforge$invokeRegisterItemBlock(ModernBlocks.soul_campfire());
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("scaffolding"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("honey_block"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("end_rod"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("chorus_plant"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("chorus_flower"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("bubble_column"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("sweet_berry_bush"));
        viaforge$invokeRegisterItemBlock(ModernBlocks.registered("powder_snow"));

        String[] shulkerColors = {"shulker_box", "white_shulker_box", "orange_shulker_box", "magenta_shulker_box", "light_blue_shulker_box", "yellow_shulker_box", "lime_shulker_box", "pink_shulker_box", "gray_shulker_box", "light_gray_shulker_box", "cyan_shulker_box", "purple_shulker_box", "blue_shulker_box", "brown_shulker_box", "green_shulker_box", "red_shulker_box", "black_shulker_box"};
        for (String color : shulkerColors) {
            viaforge$invokeRegisterItemBlock(ModernBlocks.registered(color));
        }

        String[] modernNames = {"stonecutter", "composter", "lantern", "soul_lantern", "lectern", "grindstone", "bell", "chain", "bamboo", "tube_coral", "tube_coral_fan", "candle", "candle_cake", "sculk_sensor", "big_dripleaf", "pointed_dripstone", "amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud", "mud", "sculk_shrieker", "decorated_pot", "sniffer_egg"};
        for (String name : modernNames) {
            viaforge$invokeRegisterItemBlock(ModernBlocks.registered(name));
        }

        String[] modernCorals = {"brain_coral_block", "bubble_coral_block", "fire_coral_block", "horn_coral_block", "tube_coral_block", "dead_brain_coral_block", "dead_bubble_coral_block", "dead_fire_coral_block", "dead_horn_coral_block", "dead_tube_coral_block", "brain_coral", "bubble_coral", "fire_coral", "horn_coral", "dead_brain_coral", "dead_bubble_coral", "dead_fire_coral", "dead_horn_coral", "dead_tube_coral", "brain_coral_fan", "bubble_coral_fan", "fire_coral_fan", "horn_coral_fan", "dead_brain_coral_fan", "dead_bubble_coral_fan", "dead_fire_coral_fan", "dead_horn_coral_fan", "dead_tube_coral_fan", "brain_coral_wall_fan", "bubble_coral_wall_fan", "fire_coral_wall_fan", "horn_coral_wall_fan", "dead_brain_coral_wall_fan", "dead_bubble_coral_wall_fan", "dead_fire_coral_wall_fan", "dead_horn_coral_wall_fan", "dead_tube_coral_wall_fan"};
        for (String name : modernCorals) {
            if (!name.contains("wall_fan")) {
                viaforge$invokeRegisterItemBlock(Block.blockRegistry.getObject(new ResourceLocation(name)));
            }
        }
    }
}
