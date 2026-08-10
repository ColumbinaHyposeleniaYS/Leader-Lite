package leader.ui;

import leader.module.modules.render.FontManager;

public final class GuiText {

    private GuiText() {
    }

    public static void draw(String text, float x, float y, int color) {
        FontManager.drawString(text, x, y, color, false);
    }

    public static void drawShadow(String text, float x, float y, int color) {
        FontManager.drawStringWithShadow(text, x, y, color);
    }

    public static int width(String text) {
        return FontManager.getStringWidth(text);
    }

    public static int height() {
        return FontManager.getFontHeight();
    }

    public static String trim(String text, int maxWidth) {
        if (width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = width(suffix);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = result.toString() + text.charAt(i);
            if (width(candidate) + suffixWidth > maxWidth) break;
            result.append(text.charAt(i));
        }
        return result.append(suffix).toString();
    }

    public static String trimLabelValue(String name, String value, int maxWidth) {
        int valueWidth = width(value);
        return trim(name, Math.max(0, maxWidth - valueWidth)) + value;
    }

    public static void drawCentered(String text, float centerX, float y, int color) {
        draw(text, centerX - width(text) / 2.0F, y, color);
    }
}
