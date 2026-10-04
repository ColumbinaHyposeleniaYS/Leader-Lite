package leader.ui.clickgui.augustus.component;

import leader.module.Module;
import leader.module.modules.render.FontManager;
import leader.ui.clickgui.augustus.AugustusClickGui;

import java.awt.Color;

public class ModuleComponent {
    private static final float SIDEBAR_WIDTH = 90.0F;
    private final AugustusClickGui gui;
    private final Module module;

    public ModuleComponent(AugustusClickGui gui, Module module) {
        this.gui = gui;
        this.module = module;
    }

    public float getHeight() {
        return FontManager.getFontHeight(AugustusClickGui.FONT_SIZE) + 2.0F;
    }

    public void drawScreen(int mouseX, int mouseY, float x, float y) {
        int col = module.isEnabled() ? gui.getAccent().getRGB() : new Color(200, 200, 200).getRGB();
        float textX = x + 8.0F;
        float textY = y - 4.0F;
        if (module == gui.getSelectedModule()) {
            FontManager.drawString(">", textX, textY, col, false, AugustusClickGui.FONT_SIZE);
            textX += FontManager.getStringWidth(">", AugustusClickGui.FONT_SIZE) + 2.0F;
        }
        FontManager.drawString(module.getName(), textX, textY, col, false, AugustusClickGui.FONT_SIZE);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, float x, float y) {
        float h = FontManager.getFontHeight(AugustusClickGui.FONT_SIZE);
        float tx = x + 8.0F;
        if (mouseX >= tx && mouseX <= tx + SIDEBAR_WIDTH && mouseY >= y - 3.0F && mouseY <= y - 3.0F + h + 2.0F) {
            if (mouseButton == 0) {
                module.toggle();
                return true;
            }
            if (mouseButton == 1) {
                gui.selectModule(module);
                return true;
            }
        }
        return false;
    }
}
