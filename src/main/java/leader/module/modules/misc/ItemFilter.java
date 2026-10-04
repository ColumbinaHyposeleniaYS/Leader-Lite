package leader.module.modules.misc;

import leader.Leader;
import leader.module.Module;
import leader.property.properties.ModeProperty;
import leader.property.properties.TextProperty;
import leader.util.ItemUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified per-item take/keep filter shared by ChestStealer (take from chests)
 * and InvManager (stash/organize + drop protection).
 *
 * Blacklist mode: listed items are never taken/kept (protected from being
 * picked up into the inventory, but InvManager will also refuse to drop them,
 * so they don't get destroyed either).
 * Whitelist mode: only listed items are taken/kept.
 *
 * Matching supports, for every entry:
 *  - registry name with or without the "minecraft:" prefix (e.g. minecraft:totem_of_undying / totem_of_undying)
 *  - the ViaBackwards modern model name resolved via ItemUtil.getViaModelName (e.g. end_crystal, mace)
 *  - the display name (color codes stripped, spaces normalized to underscores)
 * A trailing "*" works as a prefix wildcard (e.g. "shulker_box*").
 */
public class ItemFilter extends Module {
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Blacklist", "Whitelist"});
    public final TextProperty blacklist = new TextProperty("Blacklist", "totem_of_undying, end_crystal, elytra", () -> mode.getValue() == 0);
    public final TextProperty whitelist = new TextProperty("Whitelist", "", () -> mode.getValue() == 1);

    public ItemFilter() {
        super("ItemFilter", false);
    }

    private static ItemFilter instance() {
        if (Leader.moduleManager == null) {
            return null;
        }
        Module module = Leader.moduleManager.getModule(ItemFilter.class);
        return module instanceof ItemFilter ? (ItemFilter) module : null;
    }

    private static List<String> entries(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null) {
            return list;
        }
        for (String part : raw.split("[,;\n]")) {
            String entry = part.trim().toLowerCase(Locale.ROOT);
            if (!entry.isEmpty()) {
                list.add(entry);
            }
        }
        return list;
    }

    private List<String> activeEntries() {
        ItemFilter filter = instance();
        if (filter == null) {
            return new ArrayList<>();
        }
        return entries(filter.mode.getValue() == 1 ? filter.whitelist.getValue() : filter.blacklist.getValue());
    }

    private static boolean matchEntry(ItemStack stack, String entry) {
        boolean wildcard = entry.endsWith("*");
        String name = wildcard ? entry.substring(0, entry.length() - 1) : entry;
        if (name.isEmpty()) {
            return false;
        }

        // 1) registry name (with or without domain prefix)
        Item item = stack.getItem();
        if (item != null) {
            String registryName = item.getRegistryName();
            if (registryName != null) {
                String full = registryName.toLowerCase(Locale.ROOT);
                String path = full.startsWith("minecraft:") ? full.substring("minecraft:".length()) : full;
                if (wildcard ? full.startsWith(name) || path.startsWith(name)
                             : full.equals(name) || path.equals(name)) {
                    return true;
                }
            }
        }

        // 2) ViaBackwards modern model name (end_crystal, totem_of_undying, mace, ...)
        String model = ItemUtil.getViaModelName(stack);
        if (model != null && (wildcard ? model.startsWith(name) : model.equals(name))) {
            return true;
        }

        // 3) display name (color codes stripped, spaces -> underscores)
        String display = normalizeDisplay(stack.getDisplayName());
        if (!display.isEmpty() && (wildcard ? display.startsWith(name) : display.equals(name))) {
            return true;
        }
        return false;
    }

    private static String normalizeDisplay(String displayName) {
        if (displayName == null) {
            return "";
        }
        String cleaned = displayName.replaceAll("\u00A7.", "").replaceAll("&.", "");
        cleaned = cleaned.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return cleaned;
    }

    public boolean isListed(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        for (String entry : activeEntries()) {
            if (matchEntry(stack, entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ChestStealer hook: may this stack be taken out of a container?
     * Whitelist => only listed stacks; Blacklist => everything but listed stacks.
     * Returns true when the manager is absent (no restriction).
     */
    public static boolean shouldTake(ItemStack stack) {
        ItemFilter filter = instance();
        if (filter == null || stack == null) {
            return true;
        }
        boolean listed = filter.isListed(stack);
        return filter.mode.getValue() == 1 ? listed : !listed;
    }

    /**
     * InvManager hook: is this stack protected (kept/stashed, never auto-dropped)?
     * Whitelist with an empty list keeps the legacy behaviour (nothing specially kept);
     * with a non-empty list only listed stacks are kept.
     * Blacklist keeps exactly the listed stacks.
     */
    public static boolean shouldKeep(ItemStack stack) {
        ItemFilter filter = instance();
        if (filter == null || stack == null) {
            return false;
        }
        boolean listed = filter.isListed(stack);
        if (filter.mode.getValue() == 1) {
            return !entries(filter.whitelist.getValue()).isEmpty() && listed;
        }
        return listed;
    }

    /** Alias of {@link #shouldKeep} for stash/organize call sites. */
    public static boolean shouldStash(ItemStack stack) {
        return shouldKeep(stack);
    }

    /**
     * InvManager hook: may this stack be auto-dropped/trashed?
     * Exact inverse of {@link #shouldKeep}: a stack that is kept/stashed is
     * never trash, everything else may be dropped.
     * Returns true when the manager is absent (no restriction).
     */
    public static boolean shouldTrash(ItemStack stack) {
        ItemFilter filter = instance();
        if (filter == null || stack == null) {
            return true;
        }
        boolean listed = filter.isListed(stack);
        if (filter.mode.getValue() == 1) {
            return entries(filter.whitelist.getValue()).isEmpty() || !listed;
        }
        return !listed;
    }
}
