// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * Leader-Lite port helper: the Unfair client registers the modern blocks
 * (1.9-1.21) directly into the 1.8.9 block registry from its Block sources.
 * Here they are registered by {@code MixinBlock}; this class resolves the
 * registered instances lazily so trackers can reference them.
 */
public final class ModernBlocks {
    private ModernBlocks() {
    }

    public static Block registered(String name) {
        return Block.blockRegistry.getObject(new ResourceLocation(name));
    }

    public static Block dirt_path() {
        return registered("dirt_path");
    }

    public static Block campfire() {
        return registered("campfire");
    }

    public static Block soul_campfire() {
        return registered("soul_campfire");
    }

    // ------------------------------------------------------------------
    // Modern ItemBlock registration, called from MixinItem's @Inject into
    // Item.registerItems (TAIL).
    //
    // Reflection is used on purpose: Mixin 0.7.11 (JAVA_8 compat level)
    // cannot reliably rewrite intra-mixin calls for static member accessors.
    // A static @Invoker becomes a renamed synthetic proxy ("Renaming @Unique
    // method ..._$md$..." log line) and the call sites inside the same mixin
    // are left dangling (ClassInfo.requires isRenamed()+isSynthetic()), so
    // the woven Item ends up calling a method that no longer exists ->
    // NoSuchMethodError at runtime. The private static target
    // Item.registerItemBlock(Block) is therefore resolved reflectively:
    // "func_179216_c" in the SRG (production) runtime, "registerItemBlock"
    // in the MCP/dev environment.
    // ------------------------------------------------------------------

    private static final String[] EARLY_MODERN = {
        "scaffolding", "honey_block", "end_rod", "chorus_plant", "chorus_flower",
        "bubble_column", "sweet_berry_bush", "powder_snow"
    };

    private static final String[] SHULKER_BOXES = {
        "shulker_box", "white_shulker_box", "orange_shulker_box", "magenta_shulker_box",
        "light_blue_shulker_box", "yellow_shulker_box", "lime_shulker_box", "pink_shulker_box",
        "gray_shulker_box", "light_gray_shulker_box", "cyan_shulker_box", "purple_shulker_box",
        "blue_shulker_box", "brown_shulker_box", "green_shulker_box", "red_shulker_box",
        "black_shulker_box"
    };

    private static final String[] MODERN = {
        "stonecutter", "composter", "lantern", "soul_lantern", "lectern", "grindstone",
        "bell", "chain", "bamboo", "tube_coral", "tube_coral_fan", "candle", "candle_cake",
        "sculk_sensor", "big_dripleaf", "pointed_dripstone", "amethyst_cluster",
        "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud", "mud",
        "sculk_shrieker", "decorated_pot", "sniffer_egg"
    };

    private static final String[] CORALS = {
        "brain_coral_block", "bubble_coral_block", "fire_coral_block", "horn_coral_block",
        "tube_coral_block", "dead_brain_coral_block", "dead_bubble_coral_block",
        "dead_fire_coral_block", "dead_horn_coral_block", "dead_tube_coral_block",
        "brain_coral", "bubble_coral", "fire_coral", "horn_coral",
        "dead_brain_coral", "dead_bubble_coral", "dead_fire_coral", "dead_horn_coral",
        "dead_tube_coral", "brain_coral_fan", "bubble_coral_fan", "fire_coral_fan",
        "horn_coral_fan", "dead_brain_coral_fan", "dead_bubble_coral_fan",
        "dead_fire_coral_fan", "dead_horn_coral_fan", "dead_tube_coral_fan",
        "brain_coral_wall_fan", "bubble_coral_wall_fan", "fire_coral_wall_fan",
        "horn_coral_wall_fan", "dead_brain_coral_wall_fan", "dead_bubble_coral_wall_fan",
        "dead_fire_coral_wall_fan", "dead_horn_coral_wall_fan", "dead_tube_coral_wall_fan"
    };

    private static volatile Method registerItemBlock;

    public static void registerItemBlocks() {
        Method method = lookupRegisterItemBlock();
        if (method == null) {
            System.out.println("[Leader-Lite] Item.registerItemBlock(Block) not found, modern ItemBlocks not registered");
            return;
        }
        invoke(method, dirt_path());
        invoke(method, campfire());
        invoke(method, soul_campfire());
        for (String name : EARLY_MODERN) {
            invoke(method, registered(name));
        }
        for (String name : SHULKER_BOXES) {
            invoke(method, registered(name));
        }
        for (String name : MODERN) {
            invoke(method, registered(name));
        }
        for (String name : CORALS) {
            if (!name.contains("wall_fan")) {
                invoke(method, registered(name));
            }
        }
        System.out.println("[Leader-Lite] Modern ItemBlocks registered");
    }

    private static Method lookupRegisterItemBlock() {
        Method cached = registerItemBlock;
        if (cached != null) {
            return cached;
        }
        for (String name : new String[]{"func_179216_c", "registerItemBlock"}) {
            try {
                Method method = Item.class.getDeclaredMethod(name, Block.class);
                method.setAccessible(true);
                registerItemBlock = method;
                return method;
            } catch (NoSuchMethodException ignored) {
                // try next candidate
            }
        }
        return null;
    }

    private static void invoke(Method method, Block block) {
        if (block == null) {
            System.out.println("[Leader-Lite] Skipping ItemBlock registration for unregistered block");
            return;
        }
        try {
            method.invoke(null, block);
        } catch (Throwable t) {
            System.out.println("[Leader-Lite] Failed to register ItemBlock for " + block + ": " + t);
        }
    }
}
