package leader.ui.components;

import leader.Leader;
import leader.module.Module;
import leader.module.modules.render.HUD;
import leader.property.Property;
import leader.property.properties.*;
import leader.ui.Component;
import leader.ui.GuiText;
import leader.ui.dataset.impl.*;
import leader.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    public Module mod;
    public CategoryComponent category;
    public int offsetY;
    private final ArrayList<Component> settings;
    public boolean panelExpand;
    private static final int TITLE_HEIGHT = 18;
    private static final int SETTINGS_TOP_GAP = 6;

    public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
        this.mod = mod;
        this.category = category;
        this.offsetY = offsetY;
        this.settings = new ArrayList<>();
        this.panelExpand = false;
        int y = TITLE_HEIGHT + SETTINGS_TOP_GAP;
        if (!Leader.propertyManager.properties.get(mod.getClass()).isEmpty()) {
            for (Property<?> prop : Leader.propertyManager.properties.get(mod.getClass())) {
                Component component = null;
                if (prop instanceof BooleanProperty) component = new CheckBoxComponent((BooleanProperty) prop, this, y);
                else if (prop instanceof FloatProperty) component = new SliderComponent(new FloatSlider((FloatProperty) prop), this, y);
                else if (prop instanceof IntProperty) component = new SliderComponent(new IntSlider((IntProperty) prop), this, y);
                else if (prop instanceof PercentProperty) component = new SliderComponent(new PercentageSlider((PercentProperty) prop), this, y);
                else if (prop instanceof ModeProperty) component = new ModeComponent((ModeProperty) prop, this, y);
                else if (prop instanceof ColorProperty) component = new ColorSliderComponent((ColorProperty) prop, this, y);
                else if (prop instanceof TextProperty) component = new TextComponent((TextProperty) prop, this, y);
                if (component != null) {
                    settings.add(component);
                    y += component.getHeight();
                }
            }
        }
        settings.add(new BindComponent(this, y));
    }

    @Override
    public void draw(AtomicInteger offset) {
        int x = category.getX();
        int y = category.getY() + offsetY;
        int width = category.getWidth();
        int titleH = TITLE_HEIGHT;
        if (mod.isEnabled()) {
            // Soft row highlight plus a glowing accent bar.
            RenderUtil.drawRoundedRectWithGl(x + 5, y + 2.5F, x + width - 5, y + titleH - 2.5F, 4, new Color(255, 255, 255, 9).getRGB());
            RenderUtil.drawRoundedRectWithGl(x + 7, y + 4.5F, x + 12.5F, y + titleH - 4.5F, 2.75F, new Color(110, 170, 255, 60).getRGB());
            RenderUtil.drawRoundedRectWithGl(x + 8, y + 5.5F, x + 11.5F, y + titleH - 5.5F, 1.75F, new Color(120, 175, 255).getRGB());
        }
        if (panelExpand) {
            Gui.drawRect(x + 8, y + titleH - 1, x + width - 8, y + titleH, new Color(255, 255, 255, 18).getRGB());
        }
        int textColor = mod.isEnabled() ? new Color(245, 248, 252).getRGB() : new Color(180, 184, 193).getRGB();
        String displayName = trimText(mod.getName(), width - 34);
        // Center the label vertically regardless of the active font height.
        float textY = y + (TITLE_HEIGHT - GuiText.height()) / 2.0F;
        GuiText.draw(displayName, x + 17, textY, textColor);
        if (!settings.isEmpty()) {
            String arrow = panelExpand ? "v" : ">";
            GuiText.draw(arrow, x + width - 16, textY, new Color(150, 155, 166).getRGB());
        }
        if (panelExpand) {
            for (Component c : settings) {
                if (c.isVisible()) {
                    c.draw(offset);
                    offset.incrementAndGet();
                }
            }
        }
    }
    public ArrayList<Component> getSettings() {
        return settings;
    }
    @Override public void setComponentStartAt(int n) { this.offsetY = n; int y = n + TITLE_HEIGHT + SETTINGS_TOP_GAP; for (Component c : settings) { c.setComponentStartAt(y); if (c.isVisible()) y += c.getHeight(); } }
    @Override public int getHeight() { return panelExpand ? TITLE_HEIGHT + SETTINGS_TOP_GAP + settings.stream().filter(Component::isVisible).mapToInt(Component::getHeight).sum() : TITLE_HEIGHT; }
    @Override public void update(int mx, int my) { if (!panelExpand) return; for (Component c : settings) if (c.isVisible()) c.update(mx, my); }
    @Override public void mouseDown(int x, int y, int b) {
        if (isHovered(x, y)) {
            if (b == 0) mod.toggle();
            else if (b == 1) panelExpand = !panelExpand;
            return;
        }
        if (!panelExpand) return;
        for (Component c : settings) if (c.isVisible()) c.mouseDown(x, y, b);
    }
    @Override public void mouseReleased(int x, int y, int b) { if (!panelExpand) return; for (Component c : settings) if (c.isVisible()) c.mouseReleased(x,y,b); }
    @Override public void keyTyped(char ch, int k) { if (!panelExpand) return; for (Component c : settings) if (c.isVisible()) c.keyTyped(ch, k); }
    @Override public boolean isVisible() { return true; }
    public boolean contains(int x, int y) {
        return x > category.getX() + 5 && x < category.getX() + category.getWidth() - 5
                && y > category.getY() + offsetY && y < category.getY() + offsetY + getHeight();
    }
    private boolean isHovered(int x, int y) { return x > category.getX() + 5 && x < category.getX() + category.getWidth() - 5 && y > category.getY() + offsetY && y < category.getY() + 18 + offsetY; }

    private String trimText(String text, int maxWidth) {
        return GuiText.trim(text, maxWidth);
    }
}