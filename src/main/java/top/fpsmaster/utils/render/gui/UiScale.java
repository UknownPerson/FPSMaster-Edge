package top.fpsmaster.utils.render.gui;

public final class UiScale {
    private static boolean active;
    private static float layoutScale = 1.0f;
    private static float vanillaScale = 1.0f;
    private static float pixelScale = 1.0f;
    private static float projectionScale = 1.0f;
    private static float guiWidth = 0.0f;
    private static float guiHeight = 0.0f;
    private static int displayWidth;
    private static int displayHeight;

    private UiScale() {
    }

    public static void begin(
            float layoutScale,
            float vanillaScale,
            float guiWidth,
            float guiHeight,
            int displayWidth,
            int displayHeight
    ) {
        UiScale.layoutScale = layoutScale <= 0 ? 1.0f : layoutScale;
        UiScale.vanillaScale = vanillaScale <= 0 ? 1.0f : vanillaScale;
        UiScale.pixelScale = UiScale.layoutScale;
        UiScale.projectionScale = UiScale.layoutScale / UiScale.vanillaScale;
        UiScale.guiWidth = guiWidth;
        UiScale.guiHeight = guiHeight;
        UiScale.displayWidth = Math.max(displayWidth, 0);
        UiScale.displayHeight = Math.max(displayHeight, 0);
        active = true;
    }

    public static void end() {
        active = false;
        layoutScale = 1.0f;
        vanillaScale = 1.0f;
        pixelScale = 1.0f;
        projectionScale = 1.0f;
        guiWidth = 0.0f;
        guiHeight = 0.0f;
        displayWidth = 0;
        displayHeight = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getScale() {
        return pixelScale;
    }

    public static float getLayoutScale() {
        return layoutScale;
    }

    public static float getVanillaScale() {
        return vanillaScale;
    }

    public static float getPixelScale() {
        return pixelScale;
    }

    public static float getRenderScale() {
        return projectionScale;
    }

    public static float getProjectionScale() {
        return projectionScale;
    }

    public static float getGuiWidth() {
        return guiWidth;
    }

    public static float getGuiHeight() {
        return guiHeight;
    }

    public static int getDisplayWidth() {
        return displayWidth;
    }

    public static int getDisplayHeight() {
        return displayHeight;
    }

    public static float scale(float value) {
        return value;
    }

    public static int scale(int value) {
        return value;
    }

    public static float toProjection(float value) {
        return value * projectionScale;
    }

    public static int toProjectionInt(float value) {
        return Math.round(toProjection(value));
    }

    public static float toPixel(float value) {
        return value * layoutScale;
    }

    public static int toPixelInt(float value) {
        return Math.round(toPixel(value));
    }
}
