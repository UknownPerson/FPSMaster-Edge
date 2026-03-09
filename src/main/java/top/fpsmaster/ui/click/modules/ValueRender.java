package top.fpsmaster.ui.click.modules;

import top.fpsmaster.features.manager.Module;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

public abstract class ValueRender {
    public Module mod;
    public float height = 0f;

    public abstract void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom);

    public abstract void keyTyped(char typedChar, int keyCode);
}



