package leader.ui.clickgui.augustus;

import leader.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Thin draw adapter: the upstream Unfair RenderUtil uses (x, y, width, height) rect
 * semantics plus per-corner rounded rects, while leader.util.RenderUtil uses
 * (x1, y1, x2, y2) semantics and only uniform-radius rounded rects.
 */
public final class AugustusRender {

    private AugustusRender() {
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        RenderUtil.drawRect(x, y, x + width, y + height, color);
    }

    public static void drawRoundedRect(float x, float y, float width, float height,
                                       float radiusTopLeft, float radiusTopRight,
                                       float radiusBottomLeft, float radiusBottomRight, int color) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        if (radiusTopLeft <= 0.0F && radiusTopRight <= 0.0F
                && radiusBottomLeft <= 0.0F && radiusBottomRight <= 0.0F) {
            drawRect(x, y, width, height, color);
            return;
        }
        if (radiusTopLeft == radiusTopRight && radiusTopRight == radiusBottomLeft
                && radiusBottomLeft == radiusBottomRight) {
            RenderUtil.drawRoundedRect(x, y, x + width, y + height, radiusTopLeft, color);
            return;
        }

        float x2 = x + width;
        float y2 = y + height;
        float maxLeft = Math.max(radiusTopLeft, radiusBottomLeft);
        float maxRight = Math.max(radiusTopRight, radiusBottomRight);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        quad(wr, x + maxLeft, y, x2 - maxRight, y2, color);
        quad(wr, x, y + radiusTopLeft, x + maxLeft, y2 - radiusBottomLeft, color);
        quad(wr, x2 - maxRight, y + radiusTopRight, x2, y2 - radiusBottomRight, color);
        Tessellator.getInstance().draw();
        arc(x + radiusTopLeft, y + radiusTopLeft, radiusTopLeft, 180, 270, color);
        arc(x2 - radiusTopRight, y + radiusTopRight, radiusTopRight, 270, 360, color);
        arc(x + radiusBottomLeft, y2 - radiusBottomLeft, radiusBottomLeft, 90, 180, color);
        arc(x2 - radiusBottomRight, y2 - radiusBottomRight, radiusBottomRight, 0, 90, color);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void quad(WorldRenderer wr, float x1, float y1, float x2, float y2, int color) {
        if (x2 - x1 <= 0.0F || y2 - y1 <= 0.0F) {
            return;
        }
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        wr.pos(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        wr.pos(x2, y1, 0.0D).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, 0.0D).color(r, g, b, a).endVertex();
        wr.pos(x1, y2, 0.0D).color(r, g, b, a).endVertex();
    }

    private static void arc(float cx, float cy, float radius, int startAngle, int endAngle, int color) {
        if (radius <= 0.0F) {
            return;
        }
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0.0D).color(r, g, b, a).endVertex();
        int steps = Math.max(12, Math.min(28, (int) (radius * 3.0F)));
        int increment = Math.max(1, (endAngle - startAngle) / steps);
        for (int i = startAngle; i <= endAngle + increment; i += increment) {
            double rad = Math.toRadians(Math.min(i, endAngle));
            wr.pos(cx + Math.cos(rad) * radius, cy + Math.sin(rad) * radius, 0.0D).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        RenderUtil.drawLine(x1, y1, x2, y2, lineWidth, color);
    }

    public static void scissorStart(float x, float y, float w, float h) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * sf), (int) ((sr.getScaledHeight() - (y + h)) * sf), (int) (w * sf), (int) (h * sf));
    }

    public static void scissorEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
