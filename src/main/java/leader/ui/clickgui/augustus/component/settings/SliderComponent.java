package leader.ui.clickgui.augustus.component.settings;

import leader.module.modules.render.FontManager;
import leader.property.Property;
import leader.property.properties.FloatProperty;
import leader.property.properties.IntProperty;
import leader.property.properties.PercentProperty;
import leader.ui.clickgui.augustus.AugustusClickGui;
import leader.ui.clickgui.augustus.AugustusRender;
import leader.ui.clickgui.augustus.component.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.Locale;

public class SliderComponent extends Component {
    private static final double FLOAT_SLIDER_STEP = 0.01D;
    private static final float TRACK_WIDTH = 100.0F;
    private static final long DOUBLE_CLICK_INTERVAL = 500L;
    private final Property<?> property;
    private boolean dragging;
    private boolean editing;
    private String inputText = "";
    private long lastTextClickTime;

    public SliderComponent(AugustusClickGui gui, Property<?> property) {
        super(gui);
        this.property = property;
    }

    @Override
    public float getHeight() {
        return fh() + 4.0F;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    private float getMin() {
        if (property instanceof FloatProperty) {
            Float min = ((FloatProperty) property).getMinimum();
            return min == null ? 0.0F : min;
        }
        if (property instanceof IntProperty) return ((IntProperty) property).getMinimum();
        if (property instanceof PercentProperty) return ((PercentProperty) property).getMinimum();
        return 0.0F;
    }

    private float getMax() {
        if (property instanceof FloatProperty) {
            Float max = ((FloatProperty) property).getMaximum();
            return max == null ? 1.0F : max;
        }
        if (property instanceof IntProperty) return ((IntProperty) property).getMaximum();
        if (property instanceof PercentProperty) return ((PercentProperty) property).getMaximum();
        return 1.0F;
    }

    private float getValue() {
        if (property instanceof FloatProperty) return ((FloatProperty) property).getValue();
        if (property instanceof IntProperty) return ((IntProperty) property).getValue();
        if (property instanceof PercentProperty) return ((PercentProperty) property).getValue();
        return 0.0F;
    }

    private String formatValue() {
        if (property instanceof FloatProperty) {
            return String.format(Locale.ROOT, "%.2f", ((FloatProperty) property).getValue());
        }
        if (property instanceof IntProperty) return String.valueOf(((IntProperty) property).getValue());
        if (property instanceof PercentProperty) return ((PercentProperty) property).getValue() + "%";
        return "";
    }

    private void setValueFromPercent(float pct) {
        if (property instanceof FloatProperty) {
            FloatProperty fp = (FloatProperty) property;
            float min = fp.getMinimum() == null ? 0.0F : fp.getMinimum();
            float max = fp.getMaximum() == null ? 1.0F : fp.getMaximum();
            fp.setValue(getFloatSliderValue(min, max, pct));
        } else if (property instanceof IntProperty) {
            IntProperty ip = (IntProperty) property;
            int min = ip.getMinimum();
            int max = ip.getMaximum();
            ip.setValue(pct <= 0.0F ? min : pct >= 1.0F ? max : Math.round(min + (max - min) * pct));
        } else if (property instanceof PercentProperty) {
            PercentProperty pp = (PercentProperty) property;
            int min = pp.getMinimum();
            int max = pp.getMaximum();
            pp.setValue(pct <= 0.0F ? min : pct >= 1.0F ? max : Math.round(min + (max - min) * pct));
        }
    }

    private void applyInput() {
        if (inputText.isEmpty() || inputText.equals(".") || inputText.equals("-") || inputText.equals("-.")) {
            return;
        }
        try {
            if (property instanceof FloatProperty) {
                ((FloatProperty) property).setValue(Float.parseFloat(inputText));
            } else if (property instanceof IntProperty) {
                ((IntProperty) property).setValue(Integer.parseInt(inputText));
            } else if (property instanceof PercentProperty) {
                ((PercentProperty) property).setValue(Integer.parseInt(inputText));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private float getFloatSliderValue(float min, float max, float percent) {
        if (percent <= 0.0F) {
            return min;
        }
        if (percent >= 1.0F) {
            return max;
        }
        long totalSteps = Math.max(1L, Math.round(((double) max - min) / FLOAT_SLIDER_STEP));
        long selectedStep = Math.round((double) percent * totalSteps);
        double value = min + selectedStep * FLOAT_SLIDER_STEP;
        return MathHelper.clamp_float((float) value, min, max);
    }

    private float getTrackX() {
        return x + fw(property.getName() + ": ") + 2.0F;
    }

    private float getBoxHeight() {
        return Math.max(11.0F, fh() + 2.0F);
    }

    private float getBoxY() {
        return y + (fh() - getBoxHeight()) / 2.0F;
    }

    private float getValueY(float boxY) {
        return (float) Math.floor(boxY + (getBoxHeight() - fh()) / 2.0F);
    }

    private float getPreciseMouseX() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(minecraft);
        return (float) Mouse.getX() * (float) sr.getScaledWidth() / (float) minecraft.displayWidth;
    }

    private float getPercent(float mouseX, float trackX, float trackW) {
        if (mouseX <= trackX) {
            return 0.0F;
        }
        if (mouseX >= trackX + trackW) {
            return 1.0F;
        }
        return MathHelper.clamp_float((mouseX - trackX) / trackW, 0.0F, 1.0F);
    }

    private boolean isInSlider(int mouseX, int mouseY) {
        float trackX = getTrackX();
        float trackY = getBoxY();
        if (isHovered(mouseX, mouseY, trackX, trackY, TRACK_WIDTH, getBoxHeight())) {
            return true;
        }
        return isHovered(mouseX, mouseY, x, y, fw(property.getName() + ": "), fh());
    }

    private void enterEditing() {
        editing = true;
        dragging = false;
        inputText = String.valueOf(property.getValue());
    }

    private void exitEditing() {
        editing = false;
        inputText = "";
        lastTextClickTime = 0L;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        drawText(property.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());

        float trackX = getTrackX();
        float trackY = getBoxY();
        float trackH = getBoxHeight();

        if (dragging) {
            if (!Mouse.isButtonDown(0)) {
                dragging = false;
            } else {
                setValueFromPercent(getPercent(getPreciseMouseX(), trackX, TRACK_WIDTH));
            }
        }

        float min = getMin();
        float max = getMax();
        float val = getValue();
        float t = MathHelper.clamp_float((val - min) / (max - min), 0.0F, 1.0F);
        float targetLen = t * TRACK_WIDTH;

        AugustusRender.drawRoundedRect(trackX + 1.0F, trackY + 1.0F, 98.0F, trackH - 2.0F, 2.0F, 2.0F, 2.0F, 2.0F, new Color(34, 34, 34).getRGB());
        drawProgress(trackX, trackY, trackH, targetLen);
        drawValue(trackX, trackY);
    }

    private void drawProgress(float trackX, float trackY, float trackH, float progressWidth) {
        float left = trackX + 2.0F;
        float right = trackX + progressWidth - 2.0F;
        if (right > left) {
            AugustusRender.drawRect(left, trackY + 2.0F, Math.max(0.0F, right - left), trackH - 4.0F, gui.getAccent().getRGB());
        }
    }

    private void drawValue(float trackX, float trackY) {
        float centerX = trackX + TRACK_WIDTH / 2.0F;
        if (editing) {
            String fullText = inputText + "|";
            String renderText = System.currentTimeMillis() % 1000L < 500L ? fullText : inputText;
            float drawX = centerX - FontManager.getStringWidth(fullText, AugustusClickGui.FONT_SIZE) / 2.0F;
            drawText(renderText, drawX, getValueY(trackY), new Color(200, 200, 200).getRGB());
        } else {
            String value = formatValue();
            float drawX = centerX - FontManager.getStringWidth(value, AugustusClickGui.FONT_SIZE) / 2.0F;
            drawText(value, drawX, getValueY(trackY), new Color(200, 200, 200).getRGB());
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        if (editing) {
            if (!isInSlider(mouseX, mouseY)) {
                exitEditing();
            }
            return;
        }

        float trackX = getTrackX();
        float trackY = getBoxY();
        if (isHovered(mouseX, mouseY, trackX, trackY, TRACK_WIDTH, getBoxHeight())) {
            String value = formatValue();
            float centerX = trackX + TRACK_WIDTH / 2.0F;
            float textX = centerX - FontManager.getStringWidth(value, AugustusClickGui.FONT_SIZE) / 2.0F;
            float textY = getValueY(trackY);
            float textW = FontManager.getStringWidth(value, AugustusClickGui.FONT_SIZE);
            if (isHovered(mouseX, mouseY, textX, textY, textW, fh())) {
                long now = Minecraft.getSystemTime();
                if (now - lastTextClickTime < DOUBLE_CLICK_INTERVAL) {
                    enterEditing();
                    return;
                }
                lastTextClickTime = now;
                return;
            }
            dragging = true;
            setValueFromPercent(getPercent(mouseX, trackX, TRACK_WIDTH));
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!editing) {
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            exitEditing();
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (!inputText.isEmpty()) {
                inputText = inputText.substring(0, inputText.length() - 1);
                applyInput();
            }
            return;
        }
        if (typedChar >= '0' && typedChar <= '9') {
            inputText += typedChar;
            applyInput();
        } else if (typedChar == '.' && property instanceof FloatProperty && !inputText.contains(".")) {
            inputText += typedChar;
            applyInput();
        } else if (typedChar == '-' && inputText.isEmpty()) {
            inputText += typedChar;
            applyInput();
        }
    }
}
