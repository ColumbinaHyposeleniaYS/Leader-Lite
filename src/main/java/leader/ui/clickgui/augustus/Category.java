package leader.ui.clickgui.augustus;

import leader.module.Module;

/**
 * Augustus GUI category model. Leader-Lite has no central Category enum - modules are
 * plain classes grouped by their package (same strategy the built-in Leader ClickGui uses,
 * but with the missing "world" group added so no registered module is ever dropped).
 */
public enum Category {
    COMBAT("Combat", "leader.module.modules.combat"),
    PLAYER("Player", "leader.module.modules.player"),
    MOVEMENT("Movement", "leader.module.modules.movement"),
    RENDER("Render", "leader.module.modules.render"),
    WORLD("World", "leader.module.modules.world"),
    MISC("Misc", "leader.module.modules.misc"),
    LEGIT("Legit", "leader.module.modules.legit");

    private final String displayName;
    private final String packageName;

    Category(String displayName, String packageName) {
        this.displayName = displayName;
        this.packageName = packageName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public static Category fromModule(Module module) {
        String className = module.getClass().getName();
        for (Category category : values()) {
            if (className.startsWith(category.packageName)) {
                return category;
            }
        }
        return MISC;
    }
}
