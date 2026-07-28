package leader.module.modules.render;

import leader.Leader;
import leader.enums.ChatColors;
import leader.event.EventTarget;
import leader.event.types.EventType;
import leader.events.PacketEvent;
import leader.events.Render2DEvent;
import leader.module.Module;
import leader.module.modules.combat.KillAura;
import leader.module.modules.render.HUD;
import leader.util.ColorUtil;
import leader.util.RenderUtil;
import leader.util.TeamUtil;
import leader.util.TimerUtil;
import leader.util.shader.ShaderElement;
import leader.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "TRIANGLE", "BACKGROUND", "MODERN"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"});
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final FloatProperty fontScale = new FloatProperty("font-scale", 1.15F, 0.85F, 1.5F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offY = new IntProperty("offset-y", 40, -255, 255);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty backgroundHUDColor = new BooleanProperty("BackgroundHUDColor",true);
    public final ColorProperty backgroundColor = new ColorProperty("background-color", Color.BLACK.getRGB(),() -> !backgroundHUDColor.getValue());
    public final BooleanProperty head = new BooleanProperty("head", true);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true, () -> this.mode.getValue() != 2);
    public final BooleanProperty outline = new BooleanProperty("outline", false, () -> this.mode.getValue() != 2);
    public final BooleanProperty animations = new BooleanProperty("animations", true, () -> this.mode.getValue() != 2);
    public final BooleanProperty shadow = new BooleanProperty("shadow", false);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);
    public final BooleanProperty blur = new BooleanProperty("blur", false, () -> this.mode.getValue() == 2 || this.mode.getValue() == 3);

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Leader.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!(Boolean) this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Leader.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Leader.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
            case 1:
                int rgb = ((HUD) Leader.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            default:
                return new Color(-1);
        }
    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    private int getBackgroundColor() {
        HUD hud = (HUD) Leader.moduleManager.getModule(HUD.class);
        if (!backgroundHUDColor.getValue()) {
            return new Color((this.backgroundColor.getValue() >> 16) & 255, (this.backgroundColor.getValue() >> 8) & 255,
                    this.backgroundColor.getValue() & 255, this.getBackgroundAlpha()).getRGB();
        }
        else return new Color(hud.getColor(System.currentTimeMillis()).getRed(),hud.getColor(System.currentTimeMillis()).getGreen(),hud.getColor(System.currentTimeMillis()).getBlue(),this.getBackgroundAlpha()).getRGB();
    }

    private int getBackgroundOverlayColor(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), this.getBackgroundAlpha() / 3).getRGB();
    }

    private int getBackgroundAlpha() {
        return Math.round((float) this.background.getValue() / 100.0F * 255.0F);
    }

    private void drawOutline(float x1, float y1, float x2, float y2, float width, int color) {
        RenderUtil.drawLine(x1, y1, x2, y1, width, color);
        RenderUtil.drawLine(x2, y1, x2, y2, width, color);
        RenderUtil.drawLine(x2, y2, x1, y2, width, color);
        RenderUtil.drawLine(x1, y2, x1, y1, width, color);
    }

    private float getTextScale() {
        return this.fontScale.getValue();
    }

    private float getTextWidth(String text) {
        return FontManager.getStringWidth(text) * this.getTextScale();
    }

    private float getTextHeight() {
        return FontManager.getFontHeight() * this.getTextScale();
    }

    private void drawText(String text, float x, float y, int color) {
        float textScale = this.getTextScale();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(textScale, textScale, 1.0F);
        FontManager.drawString(text, 0.0F, 0.0F, color, this.shadow.getValue());
        GlStateManager.popMatrix();
    }

    private float getCardHeight() {
        return Math.max(27.0F, this.getTextHeight() * 2.0F + 9.0F);
    }

    private float getTextTop() {
        return 2.0F;
    }

    private float getSecondTextY() {
        return this.getTextTop() + this.getTextHeight() + 2.0F;
    }

    private float getHealthBarY() {
        return this.getCardHeight() - 5.0F;
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            EntityLivingBase entityLivingBase = this.target;
            this.target = this.resolveTarget();
            if (this.target != null) {
                float health = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
                float abs = this.target.getAbsorptionAmount() / 2.0F;
                float heal = this.target.getHealth() / 2.0F + abs;
                if (this.target != entityLivingBase) {
                    this.headTexture = null;
                    this.animTimer.setTime();
                    this.oldHealth = heal;
                    this.newHealth = heal;
                }
                if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
                    this.oldHealth = this.newHealth;
                    this.newHealth = heal;
                    this.maxHealth = this.target.getMaxHealth() / 2.0F;
                    if (this.oldHealth != this.newHealth) {
                        this.animTimer.reset();
                    }
                }
                ResourceLocation resourceLocation = this.getSkin(this.target);
                if (resourceLocation != null) {
                    this.headTexture = resourceLocation;
                }
                float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
                float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
                Color targetColor = this.getTargetColor(this.target);
                Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
                float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
                Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
                float targetNameWidth = this.getTextWidth(targetNameText);
                String healthText = ChatColors.formatColor(
                        String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
                );
                float healthTextWidth = this.getTextWidth(healthText);
                String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
                float statusTextWidth = this.getTextWidth(statusText);
                String healthDiffText = ChatColors.formatColor(
                        String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
                );
                float healthDiffWidth = this.getTextWidth(healthDiffText);
                if (this.mode.getValue() == 3) {
                    renderModern(scaledResolution, targetNameText, healthText, statusText, healthDiffText,
                            targetNameWidth, healthTextWidth, statusTextWidth, healthDiffWidth,
                            healthRatio, targetColor, healthBarColor, healthDeltaColor,
                            heal, health, abs);
                } else if (this.mode.getValue() == 2) {
                    renderBackground(scaledResolution, targetNameText, healthText,
                            targetNameWidth, healthTextWidth,
                            healthRatio, targetColor, healthBarColor,
                            heal, health, abs);
                } else if (this.mode.getValue() == 1) {
                    renderTriangle(scaledResolution, targetNameText, healthText, statusText, healthDiffText,
                            targetNameWidth, healthTextWidth, statusTextWidth, healthDiffWidth,
                            healthRatio, targetColor, healthBarColor, healthDeltaColor,
                            heal, health, abs);
                } else {
                float barContentWidth = Math.max(
                        targetNameWidth + (this.indicator.getValue() ? 2.0F + statusTextWidth + 2.0F : 0.0F),
                        healthTextWidth + (this.indicator.getValue() ? 2.0F + healthDiffWidth + 2.0F : 0.0F)
                );
                float cardHeight = this.getCardHeight();
                float headSize = Math.min(23.0F, cardHeight - 4.0F);
                float headIconOffset = this.head.getValue() && this.headTexture != null ? headSize + 2.0F : 0.0F;
                float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
                float posX = this.offX.getValue().floatValue() / this.scale.getValue();
                switch (this.posX.getValue()) {
                    case 1:
                        posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - barTotalWidth / 2.0F;
                        break;
                    case 2:
                        posX *= -1.0F;
                        posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - barTotalWidth;
                }
                float posY = this.offY.getValue().floatValue() / this.scale.getValue();
                switch (this.posY.getValue()) {
                    case 1:
                        posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - cardHeight / 2.0F;
                        break;
                    case 2:
                        posY *= -1.0F;
                        posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - cardHeight;
                }
                GlStateManager.pushMatrix();
                GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
                GlStateManager.translate(posX, posY, -450.0F);
                RenderUtil.enableRenderState();
                RenderUtil.drawRect(0.0F, 0.0F, barTotalWidth, cardHeight, this.getBackgroundColor());
                if (this.outline.getValue()) {
                    this.drawOutline(0.0F, 0.0F, barTotalWidth, cardHeight, 1.5F, targetColor.getRGB());
                }
                float healthBarY = this.getHealthBarY();
                RenderUtil.drawRect(headIconOffset + 2.0F, healthBarY, barTotalWidth - 2.0F, healthBarY + 3.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
                RenderUtil.drawRect(headIconOffset + 2.0F, healthBarY, headIconOffset + 2.0F + healthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), healthBarY + 3.0F, healthBarColor.getRGB());
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                this.drawText(targetNameText, headIconOffset + 2.0F, this.getTextTop(), -1);
                this.drawText(healthText, headIconOffset + 2.0F, this.getSecondTextY(), -1);
                if (this.indicator.getValue()) {
                    this.drawText(statusText, barTotalWidth - 2.0F - statusTextWidth, this.getTextTop(), healthDeltaColor.getRGB());
                    this.drawText(healthDiffText, barTotalWidth - 2.0F - healthDiffWidth, this.getSecondTextY(), ColorUtil.darker(healthDeltaColor, 0.8F).getRGB());
                }
                if (this.head.getValue() && this.headTexture != null) {
                    float headY = (cardHeight - headSize) / 2.0F;
                    GlStateManager.color(1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(this.headTexture);
                    Gui.drawScaledCustomSizeModalRect(2, (int) headY, 8.0F, 8.0F, 8, 8, (int) headSize, (int) headSize, 64.0F, 64.0F);
                    Gui.drawScaledCustomSizeModalRect(2, (int) headY, 40.0F, 8.0F, 8, 8, (int) headSize, (int) headSize, 64.0F, 64.0F);
                    GlStateManager.color(1.0F, 1.0F, 1.0F);
                }
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
                }
            }
        }
    }

    private void renderBackground(ScaledResolution scaledResolution,
                                   String targetNameText, String healthText,
                                   float targetNameWidth, float healthTextWidth,
                                   float healthRatio, Color targetColor, Color healthBarColor,
                                   float heal, float playerHealth, float abs) {
        final float barWidth = 150.0F;
        final float barHeight = this.getCardHeight();
        final float headSize = Math.min(23.0F, barHeight - 4.0F);
        boolean hasHead = this.head.getValue() && this.headTexture != null;
        float headIconOffset = hasHead ? headSize + 2.0F : 0.0F;

        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - barWidth / 2.0F;
                break;
            case 2:
                posX *= -1.0F;
                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - barWidth;
        }
        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - barHeight / 2.0F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - barHeight;
        }

        if (this.blur.getValue()) {
            final float bx = posX;
            final float by = posY;
            final float bw = barWidth;
            final float bh = barHeight;
            final float sc = this.scale.getValue();
            ShaderElement.addBlurTask(() -> {
                GlStateManager.pushMatrix();
                GlStateManager.scale(sc, sc, 1.0F);
                GlStateManager.translate(bx, by, -450.0F);
                RenderUtil.enableRenderState();
                RenderUtil.drawRect(0.0F, 0.0F, bw, bh, -1);
                RenderUtil.disableRenderState();
                GlStateManager.popMatrix();
            });
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
        GlStateManager.translate(posX, posY, -450.0F);

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0.0F, 0.0F, barWidth, barHeight, this.getBackgroundColor());

        float filledWidth = healthRatio * barWidth;
        int bgAlpha = this.getBackgroundAlpha();
        int fillAlpha = Math.min(bgAlpha * 3, 255);
        int fillColor = new Color(healthBarColor.getRed(), healthBarColor.getGreen(), healthBarColor.getBlue(), fillAlpha).getRGB();
        RenderUtil.drawRect(0.0F, 0.0F, filledWidth, barHeight, fillColor);

        if (filledWidth > 1.0F && filledWidth < barWidth - 1.0F) {
            RenderUtil.setColor(healthBarColor.getRGB());
            GL11.glLineWidth(1.5F);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(filledWidth, 0.0F);
            GL11.glVertex2f(filledWidth, barHeight);
            GL11.glEnd();
            GL11.glLineWidth(2.0F);
            GlStateManager.resetColor();
        }

        int borderColor = new Color(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), 80).getRGB();
        this.drawOutline(0.0F, 0.0F, barWidth, barHeight, 1.0F, borderColor);
        RenderUtil.disableRenderState();

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float textHeight = this.getTextHeight();
        float lineSpacing = 3.0F;
        float totalHeight = textHeight * 2.0F + lineSpacing;
        float centerTop = (barHeight - totalHeight) / 2.0F;
        float centerSecondY = centerTop + textHeight + lineSpacing;

        this.drawText(targetNameText, headIconOffset + 2.0F, centerTop, -1);

        String displayHealth = ChatColors.formatColor(String.format("&r&f%s&r", healthFormat.format(heal)));
        if (abs > 0.0F) {
            displayHealth = displayHealth + ChatColors.formatColor(String.format(" &6%s&r", healthFormat.format(abs)));
        }
        this.drawText(displayHealth, headIconOffset + 2.0F, centerSecondY, -1);

        if (hasHead) {
            float headY = (barHeight - headSize) / 2.0F;
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(2, (int) headY, 8.0F, 8.0F, 8, 8, (int) headSize, (int) headSize, 64.0F, 64.0F);
            Gui.drawScaledCustomSizeModalRect(2, (int) headY, 40.0F, 8.0F, 8, 8, (int) headSize, (int) headSize, 64.0F, 64.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void renderModern(ScaledResolution scaledResolution,
                              String targetNameText, String healthText, String statusText, String healthDiffText,
                              float targetNameWidth, float healthTextWidth, float statusTextWidth, float healthDiffWidth,
                              float healthRatio, Color targetColor, Color healthBarColor, Color healthDeltaColor,
                              float heal, float playerHealth, float abs) {
        final float cardWidth = Math.max(154.0F, Math.max(targetNameWidth, healthTextWidth) + 72.0F);
        final float cardHeight = 46.0F;
        final float radius = 7.0F;
        final float headSize = 32.0F;
        final boolean hasHead = this.head.getValue() && this.headTexture != null;

        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - cardWidth / 2.0F;
                break;
            case 2:
                posX *= -1.0F;
                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - cardWidth;
                break;
        }
        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - cardHeight / 2.0F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - cardHeight;
                break;
        }

        if (this.blur.getValue()) {
            final float bx = posX;
            final float by = posY;
            final float bw = cardWidth;
            final float bh = cardHeight;
            final float sc = this.scale.getValue();
            ShaderElement.addBlurTask(() -> {
                GlStateManager.pushMatrix();
                GlStateManager.scale(sc, sc, 1.0F);
                GlStateManager.translate(bx, by, -450.0F);
                RenderUtil.drawRoundedRectWithGl(0.0F, 0.0F, bw, bh, radius, -1);
                GlStateManager.popMatrix();
            });
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
        GlStateManager.translate(posX, posY, -450.0F);

        int shadowColor = new Color(0, 0, 0, this.shadow.getValue() ? 70 : 36).getRGB();
        int bgColor = new Color(11, 13, 19, Math.max(150, this.getBackgroundAlpha())).getRGB();
        int panelColor = new Color(255, 255, 255, 18).getRGB();
        int accentSoft = new Color(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), 58).getRGB();
        int trackColor = new Color(255, 255, 255, 36).getRGB();
        int barColor = new Color(healthBarColor.getRed(), healthBarColor.getGreen(), healthBarColor.getBlue(), 230).getRGB();

        RenderUtil.drawRoundedRectWithGl(2.0F, 3.0F, cardWidth + 2.0F, cardHeight + 3.0F, radius + 1.0F, shadowColor);
        RenderUtil.drawRoundedRectWithGl(0.0F, 0.0F, cardWidth, cardHeight, radius, bgColor);
        RenderUtil.drawRoundedRectWithGl(0.0F, 0.0F, 4.0F, cardHeight, 2.0F, targetColor.getRGB());
        RenderUtil.drawRoundedRectWithGl(6.0F, 5.0F, cardWidth - 6.0F, cardHeight - 5.0F, 5.0F, panelColor);
        RenderUtil.drawLine(8.0F, cardHeight - 7.0F, cardWidth - 8.0F, cardHeight - 7.0F, 3.0F, trackColor);
        RenderUtil.drawLine(8.0F, cardHeight - 7.0F, 8.0F + (cardWidth - 16.0F) * healthRatio, cardHeight - 7.0F, 3.0F, barColor);
        RenderUtil.drawRoundedRectWithGl(cardWidth - 30.0F, 7.0F, cardWidth - 8.0F, 20.0F, 4.0F, accentSoft);

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float textX = hasHead ? 46.0F : 12.0F;
        this.drawText(targetNameText, textX, 8.0F, -1);
        this.drawText(healthText, textX, 23.0F, new Color(230, 233, 240, 245).getRGB());
        if (this.indicator.getValue()) {
            this.drawText(statusText, cardWidth - 23.0F - statusTextWidth / 2.0F, 9.0F, healthDeltaColor.getRGB());
            this.drawText(healthDiffText, cardWidth - 10.0F - healthDiffWidth, 23.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB());
        }

        if (hasHead) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(10, 7, 8.0F, 8.0F, 8, 8, (int) headSize, (int) headSize, 64.0F, 64.0F);
            Gui.drawScaledCustomSizeModalRect(10, 7, 40.0F, 8.0F, 8, 8, (int) headSize, (int) headSize, 64.0F, 64.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void renderTriangle(ScaledResolution scaledResolution,
                                String targetNameText, String healthText, String statusText, String healthDiffText,
                                float targetNameWidth, float healthTextWidth, float statusTextWidth, float healthDiffWidth,
                                float healthRatio, Color targetColor, Color healthBarColor, Color healthDeltaColor,
                                float heal, float playerHealth, float abs) {
        final float baseWidth = 150.0F;
        final float textHeight = this.getTextHeight();
        final float height = Math.max(85.0F, 48.0F + textHeight * 2.0F);
        final float halfBase = baseWidth / 2.0F;
        final float headSize = 20.0F;
        final float barLineWidth = 3.5F;

        float triPosX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                triPosX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - halfBase;
                break;
            case 2:
                triPosX *= -1.0F;
                triPosX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - baseWidth;
                break;
        }
        float triPosY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                triPosY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - height / 2.0F;
                break;
            case 2:
                triPosY *= -1.0F;
                triPosY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - height;
                break;
        }
        final float tipX  = triPosX + halfBase;
        final float tipY  = triPosY;
        final float leftX = triPosX;
        final float rightX = triPosX + baseWidth;
        final float baseY = triPosY + height;

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
        GlStateManager.translate(0.0F, 0.0F, -450.0F);

        if (this.shadow.getValue()) {
            RenderUtil.enableRenderState();
            RenderUtil.drawFilledTriangle(tipX, tipY + 2.0F, leftX + 2.0F, baseY + 3.0F, rightX - 2.0F, baseY + 3.0F,
                    new Color(0, 0, 0, 20).getRGB());
            RenderUtil.disableRenderState();
        }

        RenderUtil.enableRenderState();
        int bgBaseColor = this.getBackgroundColor();
        int tipAccentColor = this.getBackgroundOverlayColor(targetColor);
        RenderUtil.drawGradientTriangle(tipX, tipY, leftX, baseY, rightX, baseY, tipAccentColor, bgBaseColor);
        RenderUtil.disableRenderState();

        int trackColor = new Color(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), 45).getRGB();
        RenderUtil.enableRenderState();
        RenderUtil.drawTriangleOutline(tipX, tipY, leftX, baseY, rightX, baseY, barLineWidth, trackColor);
        int barEmptyColor = ColorUtil.darker(healthBarColor, 0.3F).getRGB();
        RenderUtil.drawTriangleProgressBorder(leftX, baseY, tipX, tipY, rightX, baseY,
                healthRatio, barLineWidth, healthBarColor.getRGB(), barEmptyColor);
        RenderUtil.disableRenderState();

        if (this.outline.getValue()) {
            RenderUtil.enableRenderState();
            RenderUtil.drawTriangleOutline(tipX, tipY, leftX, baseY, rightX, baseY, 2.0F, targetColor.getRGB());
            RenderUtil.drawTriangleOutline(tipX, tipY, leftX - 0.5F, baseY + 0.5F, rightX + 0.5F, baseY + 0.5F, 4.0F,
                    new Color(targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue(), 50).getRGB());
            RenderUtil.disableRenderState();
        }

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final float nameY = tipY + 38.0F;
        final float infoY = nameY + textHeight + 2.0F;
        final float hwName = halfBase * (nameY - tipY) / height;
        final float hwInfo = halfBase * (infoY - tipY) / height;
        final float textMargin = 5.0F;

        this.drawText(targetNameText,
                tipX - hwName + textMargin, nameY, -1);
        if (this.indicator.getValue()) {
            this.drawText(statusText,
                    tipX + hwName - textMargin - statusTextWidth, nameY,
                    healthDeltaColor.getRGB());
        }

        String finalHealthText = ChatColors.formatColor(
                String.format("&r&f%s%s❤&r",
                        new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US)).format(heal),
                        abs > 0.0F ? "&6" : "&c")
        );
        this.drawText(finalHealthText,
                tipX - hwInfo + textMargin, infoY, -1);
        if (this.indicator.getValue()) {
            String diffStr = ChatColors.formatColor(
                    String.format("&r%s&r", heal == playerHealth ? "0.0"
                            : new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US))
                                    .format(playerHealth - heal))
            );
            float diffW = this.getTextWidth(diffStr);
            this.drawText(diffStr,
                    tipX + hwInfo - textMargin - diffW, infoY,
                    ColorUtil.darker(healthDeltaColor, 0.8F).getRGB());
        }

        if (this.head.getValue() && this.headTexture != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(this.headTexture);
            final float headX = tipX - headSize / 2.0F;
            final float headY = tipY + 14.0F;
            Gui.drawScaledCustomSizeModalRect((int) headX, (int) headY, 8.0F, 8.0F, 8, 8,
                    (int) headSize, (int) headSize, 64.0F, 64.0F);
            Gui.drawScaledCustomSizeModalRect((int) headX, (int) headY, 40.0F, 8.0F, 8, 8,
                    (int) headSize, (int) headSize, 64.0F, 64.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}
