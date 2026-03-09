package top.fpsmaster.utils.render.gui;

import java.util.ArrayList;
import java.util.List;

public final class GuiInputState {
    public static final class MouseButtonEvent {
        private final int x;
        private final int y;
        private final int button;
        private boolean consumed;

        private MouseButtonEvent(int x, int y, int button) {
            this.x = x;
            this.y = y;
            this.button = button;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getButton() {
            return button;
        }

        public boolean isConsumed() {
            return consumed;
        }

        private void consume() {
            this.consumed = true;
        }
    }

    private final List<MouseButtonEvent> pressEvents = new ArrayList<>();
    private final boolean[] buttonsDown = new boolean[8];

    private MouseButtonEvent latestPress;
    private int mouseX;
    private int mouseY;
    private int wheelDelta;

    public void updateMousePosition(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void pressButton(int button, int mouseX, int mouseY) {
        if (button >= 0 && button < buttonsDown.length) {
            buttonsDown[button] = true;
        }
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        pressEvents.add(event);
        latestPress = event;
    }

    public void releaseButton(int button) {
        if (button >= 0 && button < buttonsDown.length) {
            buttonsDown[button] = false;
        }
    }

    public boolean isButtonDown(int button) {
        return button >= 0 && button < buttonsDown.length && buttonsDown[button];
    }

    public void addWheelDelta(int wheelDelta) {
        this.wheelDelta += wheelDelta;
    }

    public int getWheelDelta() {
        return wheelDelta;
    }

    public int consumeWheelDelta() {
        int currentWheelDelta = wheelDelta;
        wheelDelta = 0;
        return currentWheelDelta;
    }

    public boolean hasPressEvent() {
        return !pressEvents.isEmpty();
    }

    public MouseButtonEvent getLatestPress() {
        return latestPress;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public MouseButtonEvent consumePressInBounds(float x, float y, float width, float height) {
        return consumePressInBounds(x, y, width, height, -1);
    }

    public MouseButtonEvent consumePressInBounds(float x, float y, float width, float height, int button) {
        for (MouseButtonEvent event : pressEvents) {
            if (event.isConsumed()) {
                continue;
            }
            if (button >= 0 && event.getButton() != button) {
                continue;
            }
            if (event.getX() < x || event.getX() > x + width || event.getY() < y || event.getY() > y + height) {
                continue;
            }
            event.consume();
            return event;
        }
        return null;
    }

    public void finishFrame() {
        pressEvents.clear();
        latestPress = null;
        wheelDelta = 0;
    }

    public void reset() {
        for (int i = 0; i < buttonsDown.length; i++) {
            buttonsDown[i] = false;
        }
        mouseX = 0;
        mouseY = 0;
        finishFrame();
    }
}
