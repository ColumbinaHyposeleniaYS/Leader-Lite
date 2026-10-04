package leader.ui.clickgui.augustus.component.settings;

import leader.property.properties.ColorProperty;
import leader.ui.clickgui.augustus.AugustusClickGui;
import leader.ui.clickgui.augustus.AugustusRender;
import leader.ui.clickgui.augustus.component.Component;
import org.lwjgl.input.Mouse;

import java.awt.Color;

public class ColorPickerComponent extends Component {
    private final ColorProperty property;
    private float hue;
    private float sat;
    private float bri;
    private boolean draggingHue;
    private boolean draggingArea;

    public ColorPickerComponent(AugustusClickGui gui, ColorProperty property) {
        super(gui);
        this.property = property;
        int rgb = property.getValue();
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        this.hue = hsb[0];
        this.sat = hsb[1];
        this.bri = hsb[2];
    }

    @Override
    public float getHeight() {
        return fh() + 2.0F + 55.0F + 6.0F + 6.0F + fh() + 4.0F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        drawText(property.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());

        float pickerX = x;
        float pickerY = y + fh() + 2.0F;
        float pickerW = 120.0F;
        float pickerH = 55.0F;

        AugustusRender.drawRect(pickerX, pickerY, pickerW, pickerH, Color.getHSBColor(hue, 1.0F, 1.0F).getRGB());

        for (int ix = 0; ix < pickerW; ix++) {
            float satValue = ix / pickerW;
            int alpha = (int) (255 * (1 - satValue));
            AugustusRender.drawRect(pickerX + ix, pickerY, 1.0F, pickerH, new Color(255, 255, 255, alpha).getRGB());
        }

        for (int iy = 0; iy < pickerH; iy++) {
            float briValue = 1 - (iy / pickerH);
            int alpha = (int) (255 * (1 - briValue));
            AugustusRender.drawRect(pickerX, pickerY + iy, pickerW, 1.0F, new Color(0, 0, 0, alpha).getRGB());
        }

        float hueY = pickerY + pickerH + 6.0F;
        float hueH = 6.0F;
        for (int ix = 0; ix < pickerW; ix++) {
            float h = ix / pickerW;
            AugustusRender.drawRect(pickerX + ix, hueY, 1.0F, hueH, Color.getHSBColor(h, 1.0F, 1.0F).getRGB());
        }

        float hx = pickerX + hue * pickerW;
        AugustusRender.drawRect(hx - 1.0F, hueY - 1.0F, 2.0F, hueH + 2.0F, Color.WHITE.getRGB());

        float cx = pickerX + sat * pickerW;
        float cy = pickerY + (1 - bri) * pickerH;
        AugustusRender.drawRect(cx - 2.0F, cy - 2.0F, 4.0F, 4.0F, Color.WHITE.getRGB());

        float previewX = pickerX + pickerW + 10.0F;
        AugustusRender.drawRect(previewX, pickerY, 18.0F, 18.0F, (0xFF000000 | property.getValue()));

        property.setValue(Color.HSBtoRGB(hue, sat, bri) & 0xFFFFFF);

        boolean inHue = isHovered(mouseX, mouseY, pickerX, hueY, pickerW, hueH);
        boolean inArea = isHovered(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH);

        if (!Mouse.isButtonDown(0)) {
            draggingHue = false;
            draggingArea = false;
        } else {
            if (inHue) {
                draggingHue = true;
                draggingArea = false;
            } else if (inArea) {
                draggingArea = true;
                draggingHue = false;
            }
        }

        if (draggingHue) {
            hue = clamp((mouseX - pickerX) / pickerW, 0.0F, 1.0F);
        } else if (draggingArea) {
            sat = clamp((mouseX - pickerX) / pickerW, 0.0F, 1.0F);
            bri = clamp(1.0F - ((mouseY - pickerY) / pickerH), 0.0F, 1.0F);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        draggingHue = false;
        draggingArea = false;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
