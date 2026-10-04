package leader.ui.clickgui.augustus.panel;

import leader.Leader;
import leader.module.Module;
import leader.ui.clickgui.augustus.AugustusClickGui;
import leader.ui.clickgui.augustus.Category;
import leader.ui.clickgui.augustus.component.ModuleComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CategoryPanel {
    private final AugustusClickGui gui;
    private final Category category;
    private final List<ModuleComponent> modules = new ArrayList<>();

    public CategoryPanel(AugustusClickGui gui, Category category) {
        this.gui = gui;
        this.category = category;
        List<Module> list = new ArrayList<>();
        for (Module module : Leader.moduleManager.modules.values()) {
            if (Category.fromModule(module) == category) {
                list.add(module);
            }
        }
        list.sort(Comparator.comparing(m -> m.getName().toLowerCase(Locale.ROOT)));
        for (Module module : list) {
            modules.add(new ModuleComponent(gui, module));
        }
    }

    public Category getCategory() {
        return category;
    }

    public float getContentHeight() {
        if (modules.isEmpty()) {
            return 0.0F;
        }
        return modules.size() * modules.get(0).getHeight();
    }

    public void drawScreen(int mouseX, int mouseY, float scroll) {
        float x = gui.getPosX();
        float y = gui.getPosY() + 26.0F + scroll;
        for (ModuleComponent module : modules) {
            module.drawScreen(mouseX, mouseY, x, y);
            y += module.getHeight();
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, float scroll) {
        float x = gui.getPosX();
        float y = gui.getPosY() + 26.0F + scroll;
        for (ModuleComponent module : modules) {
            if (module.mouseClicked(mouseX, mouseY, mouseButton, x, y)) {
                return true;
            }
            y += module.getHeight();
        }
        return false;
    }
}
