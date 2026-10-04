package net.minecraft.block;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

/**
 * Leader-Lite port: registers the Unfair modern blocks (1.9-1.21) into the
 * 1.8.9 block registry. Block/state IDs match the ViaVersion global ID space
 * exactly like the Unfair source version of Block.registerBlocks does.
 * Lives in the net.minecraft.block package because the block constructors are
 * package-private there (same as the Unfair source layout).
 */
public final class ModernBlockRegistrar {

    /**
     * Idempotency guard: registerAll() may be invoked both by MixinBlock's TAIL
     * callback and by ModernBlocks.registerItemBlocks() as a just-in-time
     * self-heal. Re-registering would create fresh Block instances and overwrite
     * the live entries, leaving stale duplicates behind, so only the first call
     * actually registers.
     */
    private static volatile boolean done;

    public static synchronized void registerAll() {
        if (done) {
            return;
        }
        done = true;

        register(198, "end_rod", new BlockEndRod().setModernMining(0.0F, ModernBlock.MiningTool.NONE, false).setLightLevel(0.9375F).setUnlocalizedName("endRod"));
        register(199, "chorus_plant", new BlockChorusPlant().setModernMining(0.4F, ModernBlock.MiningTool.AXE, false).setModernSwordSpeed(1.5F).setUnlocalizedName("chorusPlant"));
        register(200, "chorus_flower", new BlockChorusFlower().setModernMining(0.4F, ModernBlock.MiningTool.AXE, false).setModernSwordSpeed(1.5F).setUnlocalizedName("chorusFlower"));
        register(208, "dirt_path", new BlockDirtPath().setModernMining(0.65F, ModernBlock.MiningTool.SHOVEL, false).setStepSound(Block.soundTypeGrass).setUnlocalizedName("dirtPath"));
        register(209, "campfire", new BlockCampfire(false).setModernMining(2.0F, ModernBlock.MiningTool.AXE, false).setStepSound(Block.soundTypeWood).setUnlocalizedName("campfire"));
        register(210, "soul_campfire", new BlockCampfire(true).setModernMining(2.0F, ModernBlock.MiningTool.AXE, false).setStepSound(Block.soundTypeWood).setUnlocalizedName("soulCampfire"));
        register(211, "bubble_column", new BlockBubbleColumn().setModernMining(0.0F, ModernBlock.MiningTool.NONE, false).setUnlocalizedName("bubbleColumn"));
        register(212, "scaffolding", new BlockScaffolding().setModernMining(0.0F, ModernBlock.MiningTool.NONE, false).setStepSound(Block.soundTypeWood).setUnlocalizedName("scaffolding"));
        register(213, "sweet_berry_bush", new BlockSweetBerryBush().setModernMining(0.0F, ModernBlock.MiningTool.NONE, false).setStepSound(Block.soundTypeGrass).setUnlocalizedName("sweetBerryBush"));
        register(214, "honey_block", new BlockHoney().setModernMining(0.0F, ModernBlock.MiningTool.NONE, false).setStepSound(Block.soundTypeCloth).setUnlocalizedName("honeyBlock"));
        register(215, "powder_snow", new BlockPowderSnow().setModernMining(0.25F, ModernBlock.MiningTool.NONE, false).setStepSound(Block.soundTypeSnow).setUnlocalizedName("powderSnow"));

        String[] shulkerColors = {"shulker_box", "white_shulker_box", "orange_shulker_box", "magenta_shulker_box", "light_blue_shulker_box", "yellow_shulker_box", "lime_shulker_box", "pink_shulker_box", "gray_shulker_box", "light_gray_shulker_box", "cyan_shulker_box", "purple_shulker_box", "blue_shulker_box", "brown_shulker_box", "green_shulker_box", "red_shulker_box", "black_shulker_box"};
        for (int i = 0; i < shulkerColors.length; i++) {
            register(218 + i, shulkerColors[i], new BlockShulkerBox(i).setModernMining(2.0F, ModernBlock.MiningTool.PICKAXE, false).setUnlocalizedName(shulkerColors[i]));
        }

        register(3000, "stonecutter", new BlockModernFacingShape(net.minecraft.block.material.Material.rock, ProtocolVersion.v1_14, 11194, 11197, new double[]{0, 0, 0, 16, 9, 16}).setModernMining(3.5F, ModernBlock.MiningTool.PICKAXE, true));
        register(3001, "composter", new BlockModernComposter(11262, 11270).setModernMining(0.6F, ModernBlock.MiningTool.AXE, false));
        register(3002, "lantern", new BlockModernLantern(ProtocolVersion.v1_14, 11214, 11215).setModernMining(3.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3003, "soul_lantern", new BlockModernLantern(ProtocolVersion.v1_16, 14894, 14897).setModernMining(3.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3004, "lectern", new BlockModernLectern(11177, 11192).setModernMining(2.5F, ModernBlock.MiningTool.AXE, false));
        register(3005, "grindstone", new BlockModernGrindstone(11165, 11176).setModernMining(2.0F, ModernBlock.MiningTool.PICKAXE, true));
        register(3006, "bell", new BlockModernBell(11198, 11213).setModernMining(5.0F, ModernBlock.MiningTool.PICKAXE, false));
        register(3007, "chain", new BlockModernShape(net.minecraft.block.material.Material.iron, ProtocolVersion.v1_16, 4729, 4734, new double[]{6.5, 0, 6.5, 9.5, 16, 9.5}).setModernMining(5.0F, ModernBlock.MiningTool.PICKAXE, true));
        register(3008, "bamboo", new BlockModernShape(net.minecraft.block.material.Material.wood, ProtocolVersion.v1_14, 9116, 9127, new double[]{6.5, 0, 6.5, 9.5, 16, 9.5}).setModernMining(1.0F, ModernBlock.MiningTool.AXE, false).setModernSwordSpeed(Float.MAX_VALUE));
        register(3009, "tube_coral", new BlockModernShape(net.minecraft.block.material.Material.coral, ProtocolVersion.v1_14, 8994, 8995, new double[]{2, 0, 2, 14, 15, 14}).setModernMining(0.0F, ModernBlock.MiningTool.NONE, false));
        register(3010, "tube_coral_fan", new BlockModernShape(net.minecraft.block.material.Material.coral, ProtocolVersion.v1_14, 9014, 9015).setModernMining(0.0F, ModernBlock.MiningTool.NONE, false));
        register(3011, "tube_coral_wall_fan", new BlockModernWallCoralFan(9064).setModernMining(0.0F, ModernBlock.MiningTool.NONE, false));

        String[] coralNames = {"brain_coral_block", "bubble_coral_block", "fire_coral_block", "horn_coral_block", "tube_coral_block", "dead_brain_coral_block", "dead_bubble_coral_block", "dead_fire_coral_block", "dead_horn_coral_block", "dead_tube_coral_block", "brain_coral", "bubble_coral", "fire_coral", "horn_coral", "dead_brain_coral", "dead_bubble_coral", "dead_fire_coral", "dead_horn_coral", "dead_tube_coral", "brain_coral_fan", "bubble_coral_fan", "fire_coral_fan", "horn_coral_fan", "dead_brain_coral_fan", "dead_bubble_coral_fan", "dead_fire_coral_fan", "dead_horn_coral_fan", "dead_tube_coral_fan", "brain_coral_wall_fan", "bubble_coral_wall_fan", "fire_coral_wall_fan", "horn_coral_wall_fan", "dead_brain_coral_wall_fan", "dead_bubble_coral_wall_fan", "dead_fire_coral_wall_fan", "dead_horn_coral_wall_fan", "dead_tube_coral_wall_fan"};
        int[] coralStateRanges = {8980, 8981, 8982, 8983, 8979, 8975, 8976, 8977, 8978, 8974, 8996, 8998, 9000, 9002, 8986, 8988, 8990, 8992, 8984, 9016, 9018, 9020, 9022, 9006, 9008, 9010, 9012, 9004, 9072, 9080, 9088, 9096, 9032, 9040, 9048, 9056, 9024};
        for (int i = 0; i < coralNames.length; i++) {
            String name = coralNames[i];
            int first = coralStateRanges[i];
            int count = name.contains("wall_fan") ? 8 : (name.contains("fan") ? 2 : 1);
            double[][] shape = name.contains("block") ? new double[][]{{0, 0, 0, 16, 16, 16}} : name.contains("fan") ? new double[][]{} : new double[][]{{2, 0, 2, 14, 15, 14}};
            boolean coralBlock = name.contains("block");
            boolean deadCoral = name.startsWith("dead_");
            ModernBlock.MiningTool miningTool = coralBlock || deadCoral ? ModernBlock.MiningTool.PICKAXE : ModernBlock.MiningTool.NONE;
            boolean requiresTool = coralBlock || deadCoral;
            register(3012 + i, name, name.contains("wall_fan")
                    ? new BlockModernWallCoralFan(first).setModernMining(0.0F, miningTool, requiresTool)
                    : new BlockModernShape(net.minecraft.block.material.Material.coral, ProtocolVersion.v1_14, first, first + count - 1, shape).setModernMining(coralBlock ? 1.5F : 0.0F, miningTool, requiresTool));
        }

        register(3050, "candle", new BlockModernCandle(17358, 17373).setModernMining(0.1F, ModernBlock.MiningTool.NONE, false));
        register(3051, "candle_cake", new BlockModernCandleCake(17630).setModernMining(0.5F, ModernBlock.MiningTool.NONE, false));
        register(3052, "sculk_sensor", new BlockModernSculkSensor(17718).setModernMining(1.5F, ModernBlock.MiningTool.HOE, false));
        register(3053, "big_dripleaf", new BlockModernDripleaf(18624).setModernMining(0.1F, ModernBlock.MiningTool.AXE, false).setModernSwordSpeed(1.5F));
        register(3054, "pointed_dripstone", new BlockModernDripstone(18544).setModernMining(1.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3055, "amethyst_cluster", new BlockModernAmethyst(17666, 7, 3).setModernMining(1.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3056, "large_amethyst_bud", new BlockModernAmethyst(17678, 5, 3).setModernMining(1.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3057, "medium_amethyst_bud", new BlockModernAmethyst(17690, 4, 3).setModernMining(1.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3058, "small_amethyst_bud", new BlockModernAmethyst(17702, 3, 4).setModernMining(1.5F, ModernBlock.MiningTool.PICKAXE, false));
        register(3060, "mud", new BlockModernShape(net.minecraft.block.material.Material.ground, ProtocolVersion.v1_19, 19777, 19777, new double[]{0, 0, 0, 16, 14, 16}).setModernMining(0.5F, ModernBlock.MiningTool.SHOVEL, false));
        register(3061, "sculk_shrieker", new BlockModernShrieker(18900).setModernMining(3.0F, ModernBlock.MiningTool.HOE, false));
        register(3062, "decorated_pot", new BlockModernFacingShape(net.minecraft.block.material.Material.rock, ProtocolVersion.v1_20, 24119, 24134, new double[]{1, 0, 1, 15, 16, 15}).setModernMining(0.0F, ModernBlock.MiningTool.NONE, false));
        register(3063, "sniffer_egg", new BlockModernSnifferEgg(12659).setModernMining(0.5F, ModernBlock.MiningTool.NONE, false));
    }

    private static void register(int id, String name, Block block) {
        Block.blockRegistry.register(id, new net.minecraft.util.ResourceLocation(name), block);
    }

    private ModernBlockRegistrar() {
    }
}
