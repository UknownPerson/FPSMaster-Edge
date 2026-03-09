package top.fpsmaster.ui.common.control;

import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

public interface UiControl {
    void render(float x, float y, float width, float height, float mouseX, float mouseY);

    default void renderInScreen(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY) {
        render(x, y, width, height, mouseX, mouseY);
        ScaledGuiScreen.PointerEvent click = screen.consumePressInBounds(x, y, width, height);
        if (click != null) {
            mouseClicked(click.x, click.y, click.button);
        }
    }

    default void mouseClicked(float mouseX, float mouseY, int button) {
    }

    default void keyTyped(char typedChar, int keyCode) {
    }
}
