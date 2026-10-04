package leader.ui.clickgui.augustus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import leader.Leader;
import leader.config.Config;
import leader.module.Module;
import leader.module.modules.render.FontManager;
import leader.module.modules.render.HUD;
import leader.property.Property;
import leader.property.properties.BooleanProperty;
import leader.property.properties.ColorProperty;
import leader.property.properties.FloatProperty;
import leader.property.properties.IntProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import leader.property.properties.TextProperty;
import leader.ui.clickgui.augustus.component.Component;
import leader.ui.clickgui.augustus.component.settings.BooleanComponent;
import leader.ui.clickgui.augustus.component.settings.ColorPickerComponent;
import leader.ui.clickgui.augustus.component.settings.ModeComponent;
import leader.ui.clickgui.augustus.component.settings.SliderComponent;
import leader.ui.clickgui.augustus.component.settings.TextComponent;
import leader.ui.clickgui.augustus.panel.CategoryPanel;
import leader.util.KeyBindUtil;
import leader.util.RenderUtil;
import leader.util.shader.ShaderElement;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AugustusClickGui extends GuiScreen {
    public static final float FONT_SIZE = 16.0F;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final float SIDEBAR_WIDTH = 90.0F;
    private static final float CONTENT_TEXT_X_OFFSET = 100.0F;
    private static final float CONTENT_ACTION_X_OFFSET = 170.0F;
    private static final float ICON_SIZE = 18.0F;
    private static final float ICON_SEPARATOR = 8.0F;
    private static final float CONFIG_TITLE_BAR_HEIGHT = 15.0F;
    private static final float WINDOW_BUTTON_WIDTH = 20.0F;
    private static final int DEFAULT_BACKGROUND_ALPHA = 180;

    /**
     * Snapshot of every property value taken the first time the GUI is constructed.
     * leader.property.Property has no resetValue() (upstream Unfair API), so the Reset
     * button restores these captured defaults instead.
     */
    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>();
    private static boolean defaultsCaptured = false;

    private final File configFile = new File("./config/Leader/", "augustus-clickgui.txt");
    private final Map<Category, CategoryPanel> categoryPanels = new HashMap<>();
    private final List<Component> settingComponents = new ArrayList<>();

    private boolean dragging = false;
    private boolean resizing = false;
    private boolean waitingForKey = false;
    private boolean isGuiOpen = true;
    private float dragOffsetX, dragOffsetY;
    @Getter
    private float posX;
    @Getter
    private float posY;
    @Getter
    private float guiWidth;
    @Getter
    private float guiHeight;
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;
    private boolean positionInitialized = false;
    private float moduleScroll = 0F;
    private float valueScroll = 0F;
    private Category selectedCategory = Category.COMBAT;
    @Getter
    private Module selectedModule = null;

    private boolean configOpen = false;
    private boolean configDragging = false;
    private boolean configResizing = false;
    private float configDragX, configDragY;
    private float configPosX = -1, configPosY = -1;
    private float configWidth = 300, configHeight = 220;
    private String selectedConfig = null;
    private boolean creatingNewConfig = false;
    private String newConfigName = "";

    private boolean guiMaximized = false;
    private float guiRestoreX, guiRestoreY, guiRestoreW, guiRestoreH;
    private boolean configMaximized = false;
    private float configRestoreX, configRestoreY, configRestoreW, configRestoreH;

    public AugustusClickGui() {
        this.guiWidth = 600;
        this.guiHeight = 325;
        captureDefaults();
        this.loadLayout();
        for (Category category : Category.values()) {
            categoryPanels.put(category, new CategoryPanel(this, category));
        }
    }

    private static void captureDefaults() {
        if (defaultsCaptured) {
            return;
        }
        for (Map.Entry<Class<?>, ArrayList<Property<?>>> entry : Leader.propertyManager.properties.entrySet()) {
            String moduleKey = entry.getKey().getName();
            for (Property<?> property : entry.getValue()) {
                DEFAULT_VALUES.put(moduleKey + "#" + property.getName(), property.getValue());
            }
        }
        defaultsCaptured = true;
    }

    public Color getAccent() {
        try {
            HUD hud = (HUD) Leader.moduleManager.modules.get(HUD.class);
            if (hud != null) {
                return hud.getColor(System.currentTimeMillis(), 0);
            }
        } catch (Exception ignored) {
        }
        return new Color(140, 170, 255);
    }

    private int getBackgroundAlpha() {
        // Upstream read ClickGui#backgroundOpacity (Unfair-only property); Leader-Lite's
        // GuiModule has no such option, so fall back to the upstream default alpha.
        return DEFAULT_BACKGROUND_ALPHA;
    }

    public void selectModule(Module module) {
        selectedModule = module;
        valueScroll = 0;
        waitingForKey = false;
        rebuildSettings();
    }

    private static boolean isHovered(int mouseX, int mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private float fw(String text) {
        return FontManager.getStringWidth(text, FONT_SIZE);
    }

    private float fh() {
        return FontManager.getFontHeight(FONT_SIZE);
    }

    private float valueHeaderHeight(float initialValueY) {
        return (initialValueY + 8.0F) - (posY + 40.0F) + fh() + 2.0F + fh() + 10.0F;
    }

    private float valueListStartY(float initialValueY) {
        return initialValueY - 4.0F + valueHeaderHeight(initialValueY);
    }

    private float valueClipTop(float initialValueY) {
        return posY + 30.0F + valueHeaderHeight(initialValueY) + 1.5F + 1.0F;
    }

    private float valueClipHeight(float initialValueY) {
        return guiHeight - (31.0F + valueHeaderHeight(initialValueY) + 1.5F);
    }

    private boolean isInValueClip(int mouseX, int mouseY, float initialValueY) {
        float clipTop = valueClipTop(initialValueY);
        return isHovered(mouseX, mouseY, posX + SIDEBAR_WIDTH + 2.0F, clipTop, guiWidth - SIDEBAR_WIDTH, valueClipHeight(initialValueY));
    }

    private float getModuleContentHeight() {
        CategoryPanel panel = categoryPanels.get(selectedCategory);
        return panel == null ? 0.0F : panel.getContentHeight();
    }

    private float getModuleViewportHeight() {
        return Math.max(0.0F, guiHeight - 26.0F);
    }

    private float getValueContentHeight() {
        float h = 0.0F;
        for (Component c : settingComponents) {
            if (c.isVisible()) {
                h += c.getHeight();
            }
        }
        return h;
    }

    private float getValueViewportHeight(float initialValueY) {
        float contentStart = valueListStartY(initialValueY);
        float inset = contentStart - valueClipTop(initialValueY);
        return Math.max(0.0F, valueClipHeight(initialValueY) - inset);
    }

    private float clampScroll(float scroll, float contentHeight, float viewportHeight) {
        float minScroll = Math.min(0.0F, viewportHeight - contentHeight);
        return MathHelper.clamp_float(scroll, minScroll, 0.0F);
    }

    private void clampScrolls() {
        moduleScroll = clampScroll(moduleScroll, getModuleContentHeight(), getModuleViewportHeight());

        if (selectedModule == null) {
            valueScroll = 0.0F;
        } else {
            float initialValueY = posY + 40.0F;
            valueScroll = clampScroll(valueScroll, getValueContentHeight(), getValueViewportHeight(initialValueY));
        }
    }

    private void updateScreenBounds() {
        int screenWidth = super.width;
        int screenHeight = super.height;
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        boolean screenChanged = screenWidth != this.lastScreenWidth || screenHeight != this.lastScreenHeight;
        float centerXRatio = 0.5F;
        float centerYRatio = 0.5F;

        if (this.positionInitialized && screenChanged && this.lastScreenWidth > 0 && this.lastScreenHeight > 0) {
            centerXRatio = MathHelper.clamp_float((this.posX + this.guiWidth * 0.5F) / (float) this.lastScreenWidth, 0.0F, 1.0F);
            centerYRatio = MathHelper.clamp_float((this.posY + this.guiHeight * 0.5F) / (float) this.lastScreenHeight, 0.0F, 1.0F);
        }

        if (!this.positionInitialized) {
            this.posX = (screenWidth - this.guiWidth) * 0.5F;
            this.posY = (screenHeight - this.guiHeight) * 0.5F;
            this.positionInitialized = true;
        } else if (screenChanged && this.lastScreenWidth > 0) {
            this.posX = centerXRatio * screenWidth - this.guiWidth * 0.5F;
            this.posY = centerYRatio * screenHeight - this.guiHeight * 0.5F;
        }

        clampGuiToScreen(screenWidth, screenHeight);
        this.lastScreenWidth = screenWidth;
        this.lastScreenHeight = screenHeight;
        clampScrolls();
    }

    private void clampGuiToScreen(int screenWidth, int screenHeight) {
        this.posX = clampGuiPosition(this.posX, this.guiWidth, screenWidth);
        this.posY = clampGuiPosition(this.posY, this.guiHeight, screenHeight);
    }

    private float clampGuiPosition(float pos, float size, int screenSize) {
        float margin = 8.0F;
        if (size + margin * 2.0F >= screenSize) {
            return (screenSize - size) * 0.5F;
        }
        return MathHelper.clamp_float(pos, margin, screenSize - size - margin);
    }

    private void applyMouseWheel(int mouseX, int mouseY) {
        int wheel = Mouse.getDWheel();
        if (wheel == 0) {
            clampScrolls();
            return;
        }

        float rowHeight = fh() + 6.0F;
        float notches = Math.abs(wheel) >= 120 ? wheel / 120.0F : Math.signum(wheel);
        float scrollAmount = notches * rowHeight * 3.0F;
        if (selectedModule != null && isInValueClip(mouseX, mouseY, posY + 40.0F)) {
            valueScroll = clampScroll(valueScroll + scrollAmount, getValueContentHeight(), getValueViewportHeight(posY + 40.0F));
            return;
        }

        if (isHovered(mouseX, mouseY, posX, posY + 16.0F, SIDEBAR_WIDTH, guiHeight - 16.0F)) {
            moduleScroll = clampScroll(moduleScroll + scrollAmount, getModuleContentHeight(), getModuleViewportHeight());
            return;
        }

        clampScrolls();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        updateScreenBounds();
        rebuildSettings();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ShaderElement.addBlurTask(() -> this.renderPostProcessMask(0xFFFFFFFF));
        ShaderElement.addBloomTask(() -> this.renderPostProcessMask(0xFFFFFFFF));

        drawTopIconBar(mouseX, mouseY);

        if (isGuiOpen) {
            if (dragging) {
                if (Mouse.isButtonDown(0)) {
                    posX = mouseX - dragOffsetX;
                    posY = mouseY - dragOffsetY;
                    clampGuiToScreen(super.width, super.height);
                } else {
                    dragging = false;
                }
            }

            if (resizing) {
                if (Mouse.isButtonDown(0)) {
                    guiWidth = Math.max(420.0F, mouseX - posX);
                    guiHeight = Math.max(220.0F, mouseY - posY);
                    clampGuiToScreen(super.width, super.height);
                    clampScrolls();
                } else {
                    resizing = false;
                }
            }

            updateScreenBounds();
            applyMouseWheel(mouseX, mouseY);

            AugustusRender.drawRoundedRect(posX, posY + 15, guiWidth, guiHeight - 15, 0f, 0f, 6f, 6f, new Color(40, 39, 39, getBackgroundAlpha()).getRGB());
            AugustusRender.drawRoundedRect(posX, posY, guiWidth, 15f, 6f, 6f, 0f, 0f, new Color(34, 34, 34).getRGB());
            FontManager.drawString("CLICKGUI", posX + 5, posY + (15.0F - fh()) / 2.0F, new Color(200, 200, 200).getRGB(), false, FONT_SIZE);
            drawWindowControls(mouseX, mouseY, posX, posY, guiWidth, 15.0F);

            AugustusRender.drawRect(posX + 90, posY + 0.5f, 1.0f, guiHeight - 0.5f, new Color(34, 34, 34).getRGB());
            AugustusRender.drawRect(posX + 90, posY + 40, guiWidth - 90, 1.0f, new Color(34, 34, 34).getRGB());

            renderCategories(mouseX, mouseY);
            renderModuleList(mouseX, mouseY);
            renderValues(mouseX, mouseY);
        }

        if (configOpen) {
            drawConfigInterface(mouseX, mouseY);
        }
    }

    private void drawTopIconBar(int mouseX, int mouseY) {
        float totalWidth = ICON_SIZE * 2 + ICON_SEPARATOR;
        float iconHeight = ICON_SIZE + 6;
        float iconX = (super.width - totalWidth) / 2;
        float bgY = 0;
        float iconY = bgY + 3;
        float bgX = iconX - 4;
        float halfW = (totalWidth + 8) / 2.0F;
        float rightX = bgX + halfW;

        boolean settingHovered = isHovered(mouseX, mouseY, iconX, iconY, ICON_SIZE, ICON_SIZE);
        boolean configHovered = isHovered(mouseX, mouseY, iconX + ICON_SIZE + ICON_SEPARATOR, iconY, ICON_SIZE, ICON_SIZE);

        int settingBg = settingHovered ? new Color(220, 45, 45).getRGB() : new Color(34, 34, 34, 220).getRGB();
        int configBg = configHovered ? new Color(220, 45, 45).getRGB() : new Color(34, 34, 34, 220).getRGB();

        AugustusRender.drawRoundedRect(bgX, bgY, halfW, iconHeight, 0, 0, 3, 0, settingBg);
        AugustusRender.drawRoundedRect(rightX, bgY, halfW, iconHeight, 0, 0, 0, 3, configBg);

        int settingColor = new Color(200, 200, 200).getRGB();
        if (isGuiOpen) {
            settingColor = getAccent().getRGB();
        } else if (settingHovered) {
            settingColor = new Color(220, 220, 220).getRGB();
        }

        int configColor = new Color(200, 200, 200).getRGB();
        if (configOpen) {
            configColor = getAccent().getRGB();
        } else if (configHovered) {
            configColor = new Color(220, 220, 220).getRGB();
        }

        // Upstream drew tinted PNG icons (unfair/image/setting.png + config.png); those
        // assets are not shipped in Leader-Lite, so equivalent glyphs are drawn as lines.
        drawSettingIcon(iconX, iconY, settingColor);
        drawConfigIcon(iconX + ICON_SIZE + ICON_SEPARATOR, iconY, configColor);
    }

    private void drawSettingIcon(float x, float y, int color) {
        float rowGap = ICON_SIZE / 3.0F;
        for (int i = 0; i < 3; i++) {
            float ly = y + rowGap * i + rowGap / 2.0F;
            RenderUtil.drawLine(x + 1.0F, ly, x + ICON_SIZE - 1.0F, ly, 1.0F, color);
            float knobX = x + (i == 0 ? ICON_SIZE * 0.65F : i == 1 ? ICON_SIZE * 0.35F : ICON_SIZE * 0.5F);
            RenderUtil.drawLine(knobX, ly - 2.5F, knobX, ly + 2.5F, 1.5F, color);
        }
    }

    private void drawConfigIcon(float x, float y, int color) {
        float bodyTop = y + 5.0F;
        float bottom = y + ICON_SIZE - 2.0F;
        float left = x + 1.0F;
        float right = x + ICON_SIZE - 1.0F;
        RenderUtil.drawLine(left, bodyTop, right, bodyTop, 1.0F, color);
        RenderUtil.drawLine(left, bottom, right, bottom, 1.0F, color);
        RenderUtil.drawLine(left, bodyTop, left, bottom, 1.0F, color);
        RenderUtil.drawLine(right, bodyTop, right, bottom, 1.0F, color);
        float tabStart = left + ICON_SIZE * 0.2F;
        float tabTop = y + 2.0F;
        float tabEnd = tabStart + ICON_SIZE * 0.35F;
        RenderUtil.drawLine(tabStart, bodyTop, tabStart, tabTop, 1.0F, color);
        RenderUtil.drawLine(tabStart, tabTop, tabEnd, tabTop, 1.0F, color);
        RenderUtil.drawLine(tabEnd, tabTop, tabEnd + 1.0F, bodyTop, 1.0F, color);
    }

    private boolean topIconBarClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }
        float totalWidth = ICON_SIZE * 2 + ICON_SEPARATOR;
        float iconX = (super.width - totalWidth) / 2;
        float bgY = 0;
        float iconY = bgY + 3;

        if (isHovered(mouseX, mouseY, iconX, iconY, ICON_SIZE, ICON_SIZE)) {
            isGuiOpen = !isGuiOpen;
            if (!isGuiOpen) {
                this.dragging = false;
            }
            return true;
        }
        if (isHovered(mouseX, mouseY, iconX + ICON_SIZE + ICON_SEPARATOR, iconY, ICON_SIZE, ICON_SIZE)) {
            configOpen = !configOpen;
            return true;
        }
        return false;
    }

    private enum WindowAction {
        NONE,
        CLOSE,
        MAXIMIZE,
        MINIMIZE
    }

    private void drawWindowControls(int mouseX, int mouseY, float winX, float winY, float winW, float winH) {
        float btnW = WINDOW_BUTTON_WIDTH;
        float right = winX + winW;
        drawWindowControlButton(mouseX, mouseY, right - btnW, winY, btnW, winH, WindowAction.CLOSE);
        drawWindowControlButton(mouseX, mouseY, right - btnW * 2, winY, btnW, winH, WindowAction.MAXIMIZE);
        drawWindowControlButton(mouseX, mouseY, right - btnW * 3, winY, btnW, winH, WindowAction.MINIMIZE);
    }

    private void drawWindowControlButton(int mouseX, int mouseY, float x, float y, float w, float h, WindowAction action) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, h);
        if (hovered && action == WindowAction.CLOSE) {
            AugustusRender.drawRoundedRect(x, y, w, h, 0, 6, 0, 0, new Color(220, 45, 45, 180).getRGB());
        } else if (hovered) {
            AugustusRender.drawRect(x, y, w, h, new Color(255, 255, 255, 30).getRGB());
        }
        int color = hovered ? new Color(255, 255, 255).getRGB() : new Color(150, 150, 150).getRGB();
        float cx = x + w / 2.0F;
        float cy = y + h / 2.0F;
        float lw = Math.max(1.0F, new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor() * 0.5F);
        switch (action) {
            case CLOSE:
                RenderUtil.drawLine(cx - 3.0F, cy - 3.0F, cx + 3.0F, cy + 3.0F, lw, color);
                RenderUtil.drawLine(cx + 3.0F, cy - 3.0F, cx - 3.0F, cy + 3.0F, lw, color);
                break;
            case MAXIMIZE:
                RenderUtil.drawLine(cx - 3.0F, cy - 3.0F, cx + 3.0F, cy - 3.0F, lw, color);
                RenderUtil.drawLine(cx - 3.0F, cy + 3.0F, cx + 3.0F, cy + 3.0F, lw, color);
                RenderUtil.drawLine(cx - 3.0F, cy - 3.0F, cx - 3.0F, cy + 3.0F, lw, color);
                RenderUtil.drawLine(cx + 3.0F, cy - 3.0F, cx + 3.0F, cy + 3.0F, lw, color);
                break;
            case MINIMIZE:
                RenderUtil.drawLine(cx - 4.0F, cy, cx + 4.0F, cy, lw, color);
                break;
            default:
                break;
        }
    }

    private WindowAction hitWindowControl(int mouseX, int mouseY, float winX, float winY, float winW, float winH) {
        float btnW = WINDOW_BUTTON_WIDTH;
        float right = winX + winW;
        if (isHovered(mouseX, mouseY, right - btnW, winY, btnW, winH)) {
            return WindowAction.CLOSE;
        }
        if (isHovered(mouseX, mouseY, right - btnW * 2, winY, btnW, winH)) {
            return WindowAction.MAXIMIZE;
        }
        if (isHovered(mouseX, mouseY, right - btnW * 3, winY, btnW, winH)) {
            return WindowAction.MINIMIZE;
        }
        return WindowAction.NONE;
    }

    private void handleGuiWindowAction(WindowAction action) {
        switch (action) {
            case CLOSE:
            case MINIMIZE:
                isGuiOpen = false;
                this.dragging = false;
                break;
            case MAXIMIZE:
                toggleGuiMaximize();
                break;
            default:
                break;
        }
    }

    private void handleConfigWindowAction(WindowAction action) {
        switch (action) {
            case CLOSE:
            case MINIMIZE:
                configOpen = false;
                configDragging = false;
                configResizing = false;
                break;
            case MAXIMIZE:
                toggleConfigMaximize();
                break;
            default:
                break;
        }
    }

    private void toggleGuiMaximize() {
        if (!this.guiMaximized) {
            this.guiRestoreX = this.posX;
            this.guiRestoreY = this.posY;
            this.guiRestoreW = this.guiWidth;
            this.guiRestoreH = this.guiHeight;
            this.posX = 0;
            this.posY = 0;
            this.guiWidth = super.width;
            this.guiHeight = super.height;
            this.guiMaximized = true;
        } else {
            this.posX = this.guiRestoreX;
            this.posY = this.guiRestoreY;
            this.guiWidth = Math.max(420.0F, this.guiRestoreW);
            this.guiHeight = Math.max(220.0F, this.guiRestoreH);
            this.guiMaximized = false;
        }
        clampScrolls();
    }

    private void toggleConfigMaximize() {
        if (!this.configMaximized) {
            this.configRestoreX = this.configPosX;
            this.configRestoreY = this.configPosY;
            this.configRestoreW = this.configWidth;
            this.configRestoreH = this.configHeight;
            this.configPosX = 0;
            this.configPosY = 0;
            this.configWidth = super.width;
            this.configHeight = super.height;
            this.configMaximized = true;
        } else {
            this.configPosX = this.configRestoreX;
            this.configPosY = this.configRestoreY;
            this.configWidth = Math.max(300.0F, this.configRestoreW);
            this.configHeight = Math.max(220.0F, this.configRestoreH);
            this.configMaximized = false;
        }
    }

    private void renderCategories(int mouseX, int mouseY) {
        float x = posX + SIDEBAR_WIDTH + 15.0F;
        float y = posY + (55.0F - fh()) / 2.0F;
        for (Category c : Category.values()) {
            String display = c.getDisplayName();
            FontManager.drawString(display.toUpperCase(Locale.ROOT), x, y, new Color(255, 255, 255).getRGB(), false, FONT_SIZE);

            if (c == selectedCategory) {
                float lineW = fw(display.toUpperCase(Locale.ROOT));
                float lineY = y + fh() + 1.0F;
                AugustusRender.drawRect(x, lineY, lineW, 1.0F, new Color(200, 200, 200).getRGB());
            }

            x += fw(display.toUpperCase(Locale.ROOT)) + 16.0F;
        }
    }

    private void renderModuleList(int mouseX, int mouseY) {
        float listX = posX;
        float listY = posY + 16.0F;
        float listW = SIDEBAR_WIDTH;
        float listH = guiHeight - 16.0F;

        AugustusRender.scissorStart(listX, listY, listW, listH);

        CategoryPanel panel = categoryPanels.get(selectedCategory);
        if (panel != null) {
            panel.drawScreen(mouseX, mouseY, moduleScroll);
        }

        AugustusRender.scissorEnd();
    }

    private void renderValues(int mouseX, int mouseY) {
        if (selectedModule == null) {
            return;
        }

        float initialValueY = posY + 40.0F;
        float currentY = initialValueY + 8.0F;

        FontManager.drawString(selectedModule.getName() + ":", posX + CONTENT_TEXT_X_OFFSET, currentY, getAccent().getRGB(), false, FONT_SIZE);
        String resetText = "Reset";
        float resetX = posX + guiWidth - fw(resetText) - 22.0F;
        int resetCol = isHovered(mouseX, mouseY, resetX - 2.0F, currentY, fw(resetText) + 4.0F, fh())
                ? getAccent().getRGB()
                : new Color(150, 150, 150).getRGB();
        FontManager.drawString(resetText, resetX, currentY, resetCol, false, FONT_SIZE);

        currentY += fh() + 2.0F;

        String keyName = selectedModule.getKey() == 0 ? "None" : KeyBindUtil.getKeyName(selectedModule.getKey());
        int subCol = new Color(150, 150, 150).getRGB();
        if (waitingForKey) {
            FontManager.drawString("Key: ...", posX + CONTENT_TEXT_X_OFFSET, currentY + 1.0F, getAccent().getRGB(), false, FONT_SIZE);
        } else {
            FontManager.drawString("Key: " + keyName, posX + CONTENT_TEXT_X_OFFSET, currentY + 1.0F, subCol, false, FONT_SIZE);
        }

        FontManager.drawString("Hide: ", posX + CONTENT_ACTION_X_OFFSET, currentY + 1.0F, subCol, false, FONT_SIZE);
        int stateCol = selectedModule.isHidden() ? new Color(0, 180, 0).getRGB() : new Color(180, 0, 0).getRGB();
        FontManager.drawString(String.valueOf(selectedModule.isHidden()), posX + CONTENT_ACTION_X_OFFSET + fw("Hide: "), currentY + 1.0F, stateCol, false, FONT_SIZE);

        float listTop = valueClipTop(initialValueY);

        AugustusRender.scissorStart(posX + SIDEBAR_WIDTH + 1.5f + 0.5f, listTop, guiWidth - SIDEBAR_WIDTH, valueClipHeight(initialValueY));

        float y = valueListStartY(initialValueY) + valueScroll;
        for (Component c : settingComponents) {
            if (!c.isVisible()) {
                continue;
            }
            c.setX(posX + CONTENT_TEXT_X_OFFSET);
            c.setY(y);
            c.drawScreen(mouseX, mouseY);
            y += c.getHeight();
        }

        AugustusRender.scissorEnd();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (topIconBarClicked(mouseX, mouseY, mouseButton)) {
            return;
        }

        if (configOpen) {
            if (isHovered(mouseX, mouseY, configPosX, configPosY, configWidth, configHeight)) {
                configMouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
        }

        if (!isGuiOpen) {
            return;
        }

        if (mouseButton == 0) {
            WindowAction action = hitWindowControl(mouseX, mouseY, posX, posY, guiWidth, 15.0F);
            if (action != WindowAction.NONE) {
                handleGuiWindowAction(action);
                return;
            }
        }

        if (mouseButton == 0 && isHovered(mouseX, mouseY, posX, posY, guiWidth, 16)) {
            dragging = true;
            dragOffsetX = mouseX - posX;
            dragOffsetY = mouseY - posY;
            return;
        }

        if (mouseButton == 0 && isHovered(mouseX, mouseY, posX + guiWidth - 12, posY + guiHeight - 12, 14, 14)) {
            resizing = true;
            return;
        }

        float catX = posX + SIDEBAR_WIDTH + 15.0F;
        float catY = posY + (55.0F - fh()) / 2.0F;
        for (Category c : Category.values()) {
            String display = c.getDisplayName();
            float w = fw(display.toUpperCase(Locale.ROOT));
            if (isHovered(mouseX, mouseY, catX, catY, w, fh())) {
                selectedCategory = c;
                selectedModule = null;
                moduleScroll = 0;
                valueScroll = 0;
                waitingForKey = false;
                rebuildSettings();
                return;
            }
            catX += w + 16.0F;
        }

        if (isHovered(mouseX, mouseY, posX, posY + 16.0F, SIDEBAR_WIDTH, guiHeight - 16.0F)) {
            CategoryPanel panel = categoryPanels.get(selectedCategory);
            if (panel != null && panel.mouseClicked(mouseX, mouseY, mouseButton, moduleScroll)) {
                return;
            }
        }

        if (selectedModule != null) {
            float initialValueY = posY + 40.0F;
            float currentY = initialValueY + 8.0F;
            String resetText = "Reset";
            float resetX = posX + guiWidth - fw(resetText) - 22.0F;
            if (isHovered(mouseX, mouseY, resetX - 2.0F, currentY, fw(resetText) + 4.0F, fh())) {
                resetSelectedModuleProperties();
                return;
            }
            currentY += fh() + 2.0F;
            String keyName = selectedModule.getKey() == 0 ? "None" : KeyBindUtil.getKeyName(selectedModule.getKey());
            if (isHovered(mouseX, mouseY, posX + CONTENT_TEXT_X_OFFSET, currentY + 1.0F, fw("Key: " + keyName), fh())) {
                waitingForKey = !waitingForKey;
                return;
            }
            if (isHovered(mouseX, mouseY, posX + CONTENT_ACTION_X_OFFSET, currentY + 1.0F, fw("Hide: " + selectedModule.isHidden()), fh())) {
                selectedModule.setHidden(!selectedModule.isHidden());
                return;
            }

            if (isInValueClip(mouseX, mouseY, initialValueY)) {
                float py = valueListStartY(initialValueY) + valueScroll;
                for (Component c : settingComponents) {
                    if (!c.isVisible()) {
                        continue;
                    }
                    c.setX(posX + CONTENT_TEXT_X_OFFSET);
                    c.setY(py);
                    c.mouseClicked(mouseX, mouseY, mouseButton);
                    py += c.getHeight();
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (creatingNewConfig) {
            if (keyCode == 1) {
                creatingNewConfig = false;
                newConfigName = "";
            } else if (keyCode == 28) {
                if (!newConfigName.trim().isEmpty()) {
                    Config config = new Config(newConfigName.trim(), true);
                    config.save();
                    selectedConfig = newConfigName.trim();
                }
                creatingNewConfig = false;
                newConfigName = "";
            } else if (keyCode == 14) {
                if (!newConfigName.isEmpty()) {
                    newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
                }
            } else if (Character.isLetterOrDigit(typedChar) || typedChar == '_' || typedChar == '-') {
                if (newConfigName.length() < 20) {
                    newConfigName += typedChar;
                }
            }
            return;
        }

        if (keyCode == 1 && !waitingForKey) {
            mc.displayGuiScreen(null);
            return;
        }

        if (waitingForKey && selectedModule != null) {
            if (keyCode == 1) {
                selectedModule.setKey(0);
            } else {
                selectedModule.setKey(keyCode);
            }
            waitingForKey = false;
            return;
        }

        for (Component c : settingComponents) {
            c.keyTyped(typedChar, keyCode);
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        boolean changedLayout = dragging || resizing;
        dragging = false;
        resizing = false;
        configDragging = false;
        configResizing = false;
        if (changedLayout) {
            saveLayout();
        }
        for (Component c : settingComponents) {
            c.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        saveLayout();
    }

    private void rebuildSettings() {
        settingComponents.clear();
        if (selectedModule == null) {
            return;
        }
        ArrayList<Property<?>> props = Leader.propertyManager.properties.get(selectedModule.getClass());
        if (props == null) {
            return;
        }
        for (Property<?> property : props) {
            Component component = createSettingComponent(property);
            if (component != null) {
                settingComponents.add(component);
            }
        }
    }

    private Component createSettingComponent(Property<?> property) {
        if (property instanceof BooleanProperty) {
            return new BooleanComponent(this, (BooleanProperty) property);
        }
        if (property instanceof TextProperty) {
            return new TextComponent(this, (TextProperty) property);
        }
        if (property instanceof FloatProperty) {
            return new SliderComponent(this, property);
        }
        if (property instanceof IntProperty) {
            return new SliderComponent(this, property);
        }
        if (property instanceof PercentProperty) {
            return new SliderComponent(this, property);
        }
        if (property instanceof ModeProperty) {
            return new ModeComponent(this, (ModeProperty) property);
        }
        if (property instanceof ColorProperty) {
            return new ColorPickerComponent(this, (ColorProperty) property);
        }
        return null;
    }

    private void resetSelectedModuleProperties() {
        if (selectedModule == null) {
            return;
        }

        ArrayList<Property<?>> props = Leader.propertyManager.properties.get(selectedModule.getClass());
        if (props != null) {
            String moduleKey = selectedModule.getClass().getName();
            for (Property<?> property : props) {
                Object defaultValue = DEFAULT_VALUES.get(moduleKey + "#" + property.getName());
                if (defaultValue != null) {
                    property.setValue(defaultValue);
                }
            }
        }

        waitingForKey = false;
        rebuildSettings();
    }

    private void loadLayout() {
        if (!configFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            if (json.has("open")) {
                this.isGuiOpen = json.get("open").getAsBoolean();
            }
            if (json.has("x")) {
                this.posX = json.get("x").getAsFloat();
            }
            if (json.has("y")) {
                this.posY = json.get("y").getAsFloat();
            }
            if (json.has("width")) {
                this.guiWidth = Math.max(420.0F, json.get("width").getAsFloat());
            }
            if (json.has("height")) {
                this.guiHeight = Math.max(220.0F, json.get("height").getAsFloat());
            }
            if (json.has("category")) {
                try {
                    this.selectedCategory = Category.valueOf(json.get("category").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (json.has("configOpen")) {
                this.configOpen = json.get("configOpen").getAsBoolean();
            }
            if (json.has("configX")) {
                this.configPosX = json.get("configX").getAsFloat();
            }
            if (json.has("configY")) {
                this.configPosY = json.get("configY").getAsFloat();
            }
            if (json.has("configWidth")) {
                this.configWidth = Math.max(300.0F, json.get("configWidth").getAsFloat());
            }
            if (json.has("configHeight")) {
                this.configHeight = Math.max(220.0F, json.get("configHeight").getAsFloat());
            }
            this.positionInitialized = json.has("x") && json.has("y");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLayout() {
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject json = new JsonObject();
            float saveX = this.guiMaximized ? this.guiRestoreX : this.posX;
            float saveY = this.guiMaximized ? this.guiRestoreY : this.posY;
            float saveW = this.guiMaximized ? this.guiRestoreW : this.guiWidth;
            float saveH = this.guiMaximized ? this.guiRestoreH : this.guiHeight;
            json.addProperty("open", this.isGuiOpen);
            json.addProperty("x", saveX);
            json.addProperty("y", saveY);
            json.addProperty("width", saveW);
            json.addProperty("height", saveH);
            json.addProperty("category", this.selectedCategory.name());
            json.addProperty("configOpen", this.configOpen);
            json.addProperty("configX", this.configPosX);
            json.addProperty("configY", this.configPosY);
            json.addProperty("configWidth", this.configWidth);
            json.addProperty("configHeight", this.configHeight);
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private List<String> getAvailableConfigs() {
        List<String> configs = new ArrayList<>();
        File configDir = new File("./config/Leader/");
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String configName = file.getName().replace(".json", "");
                    if (configName.equals("augustus-clickgui")) {
                        continue;
                    }
                    configs.add(configName);
                }
            }
        }
        if (configs.isEmpty()) {
            configs.add("default");
        }
        return configs;
    }

    private void drawConfigInterface(int mouseX, int mouseY) {
        if (configPosX == -1 || configPosY == -1) {
            configPosX = super.width / 2f - configWidth / 2;
            configPosY = super.height / 2f - configHeight / 2;
        }

        if (configDragging) {
            configPosX = mouseX - configDragX;
            configPosY = mouseY - configDragY;
        } else if (configResizing) {
            configWidth = Math.max(300, mouseX - configPosX);
            configHeight = Math.max(220, mouseY - configPosY);
        }

        float titleBarHeight = CONFIG_TITLE_BAR_HEIGHT;
        AugustusRender.drawRoundedRect(configPosX, configPosY + titleBarHeight, configWidth, configHeight - titleBarHeight, 0f, 0f, 6f, 6f, new Color(40, 39, 39, getBackgroundAlpha()).getRGB());
        AugustusRender.drawRoundedRect(configPosX, configPosY, configWidth, titleBarHeight, 6f, 6f, 0f, 0f, new Color(34, 34, 34).getRGB());

        FontManager.drawString("CONFIGS", configPosX + 5, configPosY + (titleBarHeight - fh()) / 2.0F, new Color(200, 200, 200).getRGB(), false, FONT_SIZE);
        drawWindowControls(mouseX, mouseY, configPosX, configPosY, configWidth, titleBarHeight);

        float halfWidth = configWidth / 2.0F;
        float contentY = configPosY + titleBarHeight + 8;
        float rowGap = fh() + 3.0F;
        float margin = 12.0F;
        float innerW = halfWidth - margin * 2.0F;
        int dividerColor = new Color(34, 34, 34).getRGB();

        AugustusRender.drawRect(configPosX + halfWidth, configPosY + titleBarHeight, 1.0F, configHeight - titleBarHeight, dividerColor);

        String header = "Available Configs";
        float dividerY = contentY + fh() + rowGap;
        float headerY = (configPosY + titleBarHeight + dividerY) / 2.0F - fh() / 2.0F;
        FontManager.drawString(header, configPosX + (halfWidth - fw(header)) / 2.0F, headerY, new Color(180, 180, 180).getRGB(), false, FONT_SIZE);

        AugustusRender.drawRect(configPosX, dividerY, halfWidth, 1.0F, dividerColor);

        List<String> configs = getAvailableConfigs();
        float itemHeight = fh() + 2.0F;
        float itemY = dividerY + 3.0F;

        for (String config : configs) {
            boolean hovered = isHovered(mouseX, mouseY, configPosX, itemY, halfWidth, itemHeight);
            boolean selected = config.equals(selectedConfig);

            int textColor = selected ? Color.WHITE.getRGB() : (hovered ? getAccent().getRGB() : new Color(180, 180, 180).getRGB());
            FontManager.drawString(config, configPosX + (halfWidth - fw(config)) / 2.0F, itemY + (itemHeight - fh()) / 2.0F, textColor, false, FONT_SIZE);
            itemY += itemHeight + 3.0F;
        }

        float buttonH = 20.0F;
        float buttonGap = rowGap - 10.0F;
        String[] topButtons = {"Create", "Load", "Folder", "Refresh"};
        float btnX = configPosX + halfWidth + margin;
        float btnY = contentY;

        for (String button : topButtons) {
            boolean buttonHovered = isHovered(mouseX, mouseY, btnX, btnY, innerW, buttonH);
            int textColor = buttonHovered ? getAccent().getRGB() : new Color(200, 200, 200).getRGB();
            FontManager.drawString(button, btnX + (innerW - fw(button)) / 2.0F, btnY + (buttonH - fh()) / 2.0F, textColor, false, FONT_SIZE);
            btnY += buttonH + buttonGap;
        }

        float deleteY = configPosY + configHeight - buttonH - 3.0F;
        boolean deleteHovered = isHovered(mouseX, mouseY, btnX, deleteY, innerW, buttonH);
        int deleteColor = deleteHovered ? new Color(220, 45, 45).getRGB() : new Color(180, 180, 180).getRGB();
        FontManager.drawString("Delete", btnX + (innerW - fw("Delete")) / 2.0F, deleteY + (buttonH - fh()) / 2.0F, deleteColor, false, FONT_SIZE);

        if (creatingNewConfig) {
            float inputY = btnY + 3.0F;
            AugustusRender.drawRect(btnX, inputY, innerW, buttonH, new Color(30, 30, 30, 200).getRGB());

            String displayText = newConfigName.isEmpty() ? "Enter name..." : newConfigName;
            int inputTextColor = newConfigName.isEmpty() ? new Color(120, 120, 120).getRGB() : new Color(200, 200, 200).getRGB();
            FontManager.drawString(displayText, btnX + 4, inputY + (buttonH - fh()) / 2.0F, inputTextColor, false, FONT_SIZE);

            if (System.currentTimeMillis() % 1000 < 500) {
                float cursorX = btnX + 4 + fw(newConfigName);
                AugustusRender.drawRect(cursorX, inputY + 3, 1.0F, buttonH - 6, new Color(200, 200, 200).getRGB());
            }

            float confirmY = inputY + buttonH + 4;
            boolean confirmHovered = isHovered(mouseX, mouseY, btnX, confirmY, innerW / 2 - 2, 18);
            boolean cancelHovered = isHovered(mouseX, mouseY, btnX + innerW / 2 + 2, confirmY, innerW / 2 - 2, 18);

            Color confirmColor = confirmHovered ? new Color(0, 120, 0, 180) : new Color(0, 80, 0, 150);
            Color cancelColor = cancelHovered ? new Color(120, 0, 0, 180) : new Color(80, 0, 0, 150);

            AugustusRender.drawRoundedRect(btnX, confirmY, innerW / 2 - 2, 18, 1, 1, 1, 1, confirmColor.getRGB());
            AugustusRender.drawRoundedRect(btnX + innerW / 2 + 2, confirmY, innerW / 2 - 2, 18, 1, 1, 1, 1, cancelColor.getRGB());

            float confirmTextX = btnX + (innerW / 2 - 2) / 2 - fw("Create") / 2;
            float cancelTextX = btnX + innerW / 2 + 2 + (innerW / 2 - 2) / 2 - fw("Cancel") / 2;

            FontManager.drawString("Create", confirmTextX, confirmY + 5, Color.WHITE.getRGB(), false, FONT_SIZE);
            FontManager.drawString("Cancel", cancelTextX, confirmY + 5, Color.WHITE.getRGB(), false, FONT_SIZE);
        }
    }

    private boolean configMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (configPosX == -1 || configPosY == -1) {
            configPosX = super.width / 2f - configWidth / 2;
            configPosY = super.height / 2f - configHeight / 2;
        }
        float titleBarHeight = CONFIG_TITLE_BAR_HEIGHT;

        if (mouseButton == 0) {
            WindowAction action = hitWindowControl(mouseX, mouseY, configPosX, configPosY, configWidth, titleBarHeight);
            if (action != WindowAction.NONE) {
                handleConfigWindowAction(action);
                return true;
            }
        }

        if (mouseButton == 0 && isHovered(mouseX, mouseY, configPosX, configPosY, configWidth, titleBarHeight)) {
            configDragging = true;
            configDragX = mouseX - configPosX;
            configDragY = mouseY - configPosY;
            return true;
        }

        float resizeSize = 8;
        if (mouseButton == 0 && isHovered(mouseX, mouseY, configPosX + configWidth - resizeSize, configPosY + configHeight - resizeSize, resizeSize, resizeSize)) {
            configResizing = true;
            configDragX = mouseX;
            configDragY = mouseY;
            return true;
        }

        float halfWidth = configWidth / 2.0F;
        float contentY = configPosY + titleBarHeight + 8;
        float rowGap = fh() + 3.0F;
        float margin = 12.0F;
        float innerW = halfWidth - margin * 2.0F;

        List<String> configs = getAvailableConfigs();
        float itemHeight = fh() + 2.0F;
        float dividerY = contentY + fh() + rowGap;
        float itemY = dividerY + 3.0F;

        for (String config : configs) {
            if (isHovered(mouseX, mouseY, configPosX, itemY, halfWidth, itemHeight)) {
                selectedConfig = config;
                return true;
            }
            itemY += itemHeight + 3.0F;
        }

        float buttonH = 20.0F;
        float buttonGap = rowGap - 10.0F;
        String[] topButtons = {"Create", "Load", "Folder", "Refresh"};
        float btnX = configPosX + halfWidth + margin;
        float btnY = contentY;

        for (String button : topButtons) {
            if (isHovered(mouseX, mouseY, btnX, btnY, innerW, buttonH)) {
                handleConfigAction(button);
                return true;
            }
            btnY += buttonH + buttonGap;
        }

        float deleteY = configPosY + configHeight - buttonH - 3.0F;
        if (isHovered(mouseX, mouseY, btnX, deleteY, innerW, buttonH)) {
            handleConfigAction("Delete");
            return true;
        }

        if (creatingNewConfig) {
            float inputY = btnY + 3.0F;
            float confirmY = inputY + buttonH + 4;

            if (isHovered(mouseX, mouseY, btnX, confirmY, innerW / 2 - 2, 18)) {
                if (!newConfigName.trim().isEmpty()) {
                    Config config = new Config(newConfigName.trim(), true);
                    config.save();
                    selectedConfig = newConfigName.trim();
                }
                creatingNewConfig = false;
                newConfigName = "";
                return true;
            } else if (isHovered(mouseX, mouseY, btnX + innerW / 2 + 2, confirmY, innerW / 2 - 2, 18)) {
                creatingNewConfig = false;
                newConfigName = "";
                return true;
            }
        }
        return false;
    }

    private void handleConfigAction(String action) {
        switch (action) {
            case "Load":
                if (selectedConfig != null) {
                    new Config(selectedConfig, false).load();
                }
                break;
            case "Refresh":
                if (selectedConfig != null && !getAvailableConfigs().contains(selectedConfig)) {
                    selectedConfig = null;
                }
                break;
            case "Create":
                creatingNewConfig = true;
                newConfigName = "";
                break;
            case "Delete":
                if (selectedConfig != null) {
                    Config config = new Config(selectedConfig, false);
                    if (config.file.exists()) {
                        config.file.delete();
                    }
                    selectedConfig = null;
                }
                break;
            case "Folder":
                try {
                    Desktop.getDesktop().open(new File("./config/Leader/"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void renderPostProcessMask(int color) {
        float totalWidth = ICON_SIZE * 2 + ICON_SEPARATOR;
        float iconHeight = ICON_SIZE + 6;
        float iconX = (super.width - totalWidth) / 2;
        float bgX = iconX - 4;
        float halfW = (totalWidth + 8) / 2.0F;
        AugustusRender.drawRoundedRect(bgX, 0, halfW, iconHeight, 0, 0, 3, 0, color);
        AugustusRender.drawRoundedRect(bgX + halfW, 0, halfW, iconHeight, 0, 0, 0, 3, color);

        if (isGuiOpen) {
            AugustusRender.drawRoundedRect(posX, posY + 15.0F, guiWidth, guiHeight - 15.0F, 0.0F, 0.0F, 6.0F, 6.0F, color);
            AugustusRender.drawRoundedRect(posX, posY, guiWidth, 15.0F, 6.0F, 6.0F, 0.0F, 0.0F, color);
        }

        if (configOpen) {
            AugustusRender.drawRoundedRect(configPosX, configPosY + CONFIG_TITLE_BAR_HEIGHT, configWidth, configHeight - CONFIG_TITLE_BAR_HEIGHT, 0.0F, 0.0F, 6.0F, 6.0F, color);
            AugustusRender.drawRoundedRect(configPosX, configPosY, configWidth, CONFIG_TITLE_BAR_HEIGHT, 6.0F, 6.0F, 0.0F, 0.0F, color);
        }
    }
}
