package leader.ui.clickgui.augustus.component.settings;

import leader.property.properties.TextProperty;
import leader.ui.clickgui.augustus.AugustusClickGui;
import leader.ui.clickgui.augustus.AugustusRender;
import leader.ui.clickgui.augustus.component.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import java.awt.Color;

public class TextComponent extends Component {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TextProperty property;
    private GuiTextField textField;

    public TextComponent(AugustusClickGui gui, TextProperty property) {
        super(gui);
        this.property = property;
    }

    @Override
    public float getHeight() {
        return fh() + 4.0F;
    }

    private float getBoxX() {
        return x + fw(property.getName() + ": ");
    }

    private float getBoxY() {
        return y - 2.0F;
    }

    private float getBoxHeight() {
        return fh() + 4.0F;
    }

    private GuiTextField getTextField(float boxW, float boxH) {
        if (textField == null) {
            textField = new GuiTextField(0, mc.fontRendererObj, 0, 0, (int) boxW - 6, (int) boxH);
            textField.setMaxStringLength(64);
            textField.setFocused(false);
            textField.setText(property.getValue());
        }
        return textField;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        drawText(property.getName() + ": ", x, y, new Color(200, 200, 200).getRGB());

        float boxX = getBoxX();
        float boxW = 140.0F;
        float boxH = getBoxHeight();
        float boxY = getBoxY();

        AugustusRender.drawRect(boxX - 1.0F, boxY, boxW + 2.0F, boxH, new Color(34, 34, 34).getRGB());
        AugustusRender.drawRect(boxX, boxY + 1.0F, boxW, boxH - 2.0F, new Color(45, 45, 45, 200).getRGB());

        GuiTextField tf = getTextField(boxW, boxH);
        tf.xPosition = (int) (boxX + 3.0F);
        tf.yPosition = (int) (boxY + 2.0F);
        tf.width = (int) boxW - 6;
        tf.height = (int) boxH;
        tf.setEnableBackgroundDrawing(false);
        tf.drawTextBox();

        property.setValue(tf.getText());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float boxX = getBoxX();
        float boxW = 140.0F;
        float boxH = getBoxHeight();
        float boxY = getBoxY();
        if (textField != null) {
            textField.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (isHovered(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
            GuiTextField f = new GuiTextField(0, mc.fontRendererObj, (int) (boxX + 3.0F), (int) (boxY + 2.0F), (int) boxW - 6, (int) boxH);
            f.setMaxStringLength(64);
            f.setText(property.getValue());
            f.setFocused(true);
            textField = f;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (textField != null && textField.isFocused()) {
            textField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
