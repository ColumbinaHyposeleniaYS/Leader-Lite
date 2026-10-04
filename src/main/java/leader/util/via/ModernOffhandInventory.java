// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

import net.minecraft.item.ItemStack;

/**
 * Client-side bridge for the modern inventory slot 45.
 */
public interface ModernOffhandInventory {

    ItemStack getOffhand();

    void setOffhand(ItemStack stack);
}
