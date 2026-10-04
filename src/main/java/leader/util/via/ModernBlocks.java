// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

import net.minecraft.block.Block;
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
}
