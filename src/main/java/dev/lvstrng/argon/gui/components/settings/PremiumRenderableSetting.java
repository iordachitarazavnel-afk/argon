package dev.lvstrng.argon.gui.components.settings;

import net.minecraft.client.gui.DrawContext;

/**
 * PremiumRenderableSetting — base for all redesigned setting components.
 *
 * Each setting knows its own rendering within a given bounding rect
 * (x, y, w, h) provided at render time.  No stored absolute positions.
 */
public abstract class PremiumRenderableSetting {

    /** Index within the parent module's setting list (used for stagger). */
    public int index = 0;

    /**
     * Render within the rect (rx, ry, rw, rh).
     * @param alpha 0–1 visibility multiplier (for expand animation).
     */
    public abstract void render(DrawContext context,
                                int rx, int ry, int rw, int rh,
                                int mouseX, int mouseY, double alpha);

    public abstract void mouseClicked(double mouseX, double mouseY, int button,
                                      int rx, int ry, int rw, int rh);

    public void mouseDragged(double mouseX, double mouseY, int button,
                             double dX, double dY,
                             int rx, int ry, int rw, int rh) {}

    public void mouseReleased(double mouseX, double mouseY, int button) {}

    public void keyPressed(int keyCode, int scanCode, int modifiers) {}

    /** Return true if this setting's label/value contains the query. */
    public abstract boolean matchesQuery(String query);
}
