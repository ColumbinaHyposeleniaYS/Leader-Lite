package leader.ui.clickgui.augustus.component.settings;

import leader.property.properties.ModeProperty;
import leader.ui.clickgui.augustus.AugustusClickGui;
import leader.ui.clickgui.augustus.component.Component;

import java.awt.Color;

public class ModeComponent extends Component {
    private final ModeProperty property;

    public ModeComponent(AugustusClickGui gui, ModeProperty property) {
        super(gui);
        this.property = property;
    }

    @Override
    public float getHeight() {
        float penX = x + fw(property.getName() + ": ");
        float yy = 0.0F;
        String[] modes = property.getModes();
        for (int i = 0; i < modes.length; i++) {
            penX += fw(modes[i]);
            if (i < modes.length - 1) {
                penX += fw(", ");
            }
            if (penX > gui.getPosX() + gui.getGuiWidth() - 60.0F) {
                penX = x + fw(property.getName() + ": ");
                yy += fh() + 2.0F;
            }
        }
        return yy + fh() + 2.0F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        drawText(property.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());
        float penX = x + fw(property.getName() + ": ");
        float penY = y;
        String[] modes = property.getModes();
        for (int i = 0; i < modes.length; i++) {
            String mode = modes[i];
            boolean hovered = isHovered(mouseX, mouseY, penX, penY, fw(mode), fh());
            int col = property.getValue() == i ? gui.getAccent().getRGB() : new Color(200, 200, 200).getRGB();
            drawText(mode, penX, penY, hovered ? gui.getAccent().getRGB() : col);
            penX += fw(mode);
            if (i < modes.length - 1) {
                drawText(", ", penX, penY, new Color(200, 200, 200).getRGB());
                penX += fw(", ");
            }
            if (penX > gui.getPosX() + gui.getGuiWidth() - 60.0F) {
                penX = x + fw(property.getName() + ": ");
                penY += fh() + 2.0F;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float penX = x + fw(property.getName() + ": ");
        float penY = y;
        String[] modes = property.getModes();
        for (int i = 0; i < modes.length; i++) {
            String mode = modes[i];
            if (isHovered(mouseX, mouseY, penX, penY, fw(mode), fh())) {
                property.setValue(i);
                return;
            }
            penX += fw(mode);
            if (i < modes.length - 1) {
                penX += fw(", ");
            }
            if (penX > gui.getPosX() + gui.getGuiWidth() - 60.0F) {
                penX = x + fw(property.getName() + ": ");
                penY += fh() + 2.0F;
            }
        }
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
