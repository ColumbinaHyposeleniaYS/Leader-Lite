package leader.ui.clickgui.augustus.component.settings;

import leader.property.properties.BooleanProperty;
import leader.ui.clickgui.augustus.AugustusClickGui;
import leader.ui.clickgui.augustus.component.Component;

import java.awt.Color;

public class BooleanComponent extends Component {
    private final BooleanProperty property;

    public BooleanComponent(AugustusClickGui gui, BooleanProperty property) {
        super(gui);
        this.property = property;
    }

    @Override
    public float getHeight() {
        return fh() + 2.0F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        String name = property.getName() + ": ";
        int nameCol = new Color(200, 200, 200).getRGB();
        drawText(name, x, y, nameCol);
        boolean hovered = isHovered(mouseX, mouseY, x, y, fw(name + property.getValue()), fh());
        int vCol = property.getValue() ? new Color(0, 180, 0).getRGB() : new Color(180, 0, 0).getRGB();
        drawText(property.getValue().toString(), x + fw(name), y, hovered ? gui.getAccent().getRGB() : vCol);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        String full = property.getName() + ": " + property.getValue();
        if (isHovered(mouseX, mouseY, x, y, fw(full), fh())) {
            property.setValue(!property.getValue());
        }
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
