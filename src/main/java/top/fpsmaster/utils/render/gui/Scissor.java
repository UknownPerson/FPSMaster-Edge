package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class Scissor {
    public static void apply(float x, float y, float width, float height) {
        float scale = UiScale.isActive() ? UiScale.getLayoutScale() : 1.0f;
        applyScaled(x, y, width, height, scale);
    }

    public static void apply(float x, float y, float width, float height, float scaleFactor) {
        applyScaled(x, y, width, height, scaleFactor);
    }

    public static void apply(float x, float y, float width, float height, int scaleFactor) {
        applyScaled(x, y, width, height, 2.0f);
    }

    private static void applyScaled(float x, float y, float width, float height, float scaleFactor) {
        if (Minecraft.getMinecraft().currentScreen == null) {
            return;
        }
        int displayHeight = Minecraft.getMinecraft().displayHeight;
        int sx = Math.round(x * scaleFactor);
        int sy = Math.round(y * scaleFactor);
        int sw = Math.round(width * scaleFactor);
        int sh = Math.round(height * scaleFactor);
        GL11.glScissor(
                sx,
                displayHeight - (sy + sh),
                sw,
                sh
        );
    }
}
