package top.fpsmaster.utils.render.gui;

public final class GuiDragState {
    private Object owner;
    private int button = -1;

    public boolean isDragging() {
        return owner != null;
    }

    public boolean isDragging(Object owner) {
        return this.owner != null && this.owner.equals(owner);
    }

    public int getButton() {
        return button;
    }

    public boolean acquire(Object owner, int button) {
        if (this.owner != null && !this.owner.equals(owner)) {
            return false;
        }
        this.owner = owner;
        this.button = button;
        return true;
    }

    public void release(Object owner) {
        if (this.owner != null && this.owner.equals(owner)) {
            clear();
        }
    }

    public void clear() {
        owner = null;
        button = -1;
    }
}
