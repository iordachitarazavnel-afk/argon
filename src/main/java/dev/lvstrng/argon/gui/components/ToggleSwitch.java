package dev.lvstrng.argon.gui.components;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * ToggleSwitch — animated pill-shaped ON/OFF toggle.
 *
 * Width = 36px, Height = 20px.
 * Thumb slides smoothly with EASE_OUT_EXPO.
 * Background fades between muted and accent.
 *
 * Usage:
 *   ToggleSwitch ts = new ToggleSwitch();
 *   ts.render(context, x, y, isEnabled);
 *   boolean clicked = ts.mouseClicked(mouseX, mouseY, button, x, y);
 */
public final class ToggleSwitch {

    public static final int W = 36;
    public static final int H = 20;

    private static final int THUMB_RADIUS = 7;
    private static final int RADIUS       = 10;  // pill radius = H/2

    private final AnimationManager thumbAnim;
    private final AnimationManager colorAnim;
    private final AnimationManager glowAnim;

    private boolean lastState = false;

    public ToggleSwitch() {
        thumbAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_BACK, 14.0);
        colorAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_CUBIC, 10.0);
        glowAnim  = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
    }

    /**
     * Render the toggle at (x, y).  Call every frame.
     *
     * @param enabled  current state of the backing setting
     * @param hovered  whether the mouse is hovering over it
     */
    public void render(DrawContext context, int x, int y, boolean enabled, boolean hovered) {
        if (enabled != lastState) {
            lastState = enabled;
        }

        thumbAnim.setTarget(enabled ? 1.0 : 0.0);
        colorAnim.setTarget(enabled ? 1.0 : 0.0);
        glowAnim.setTarget(hovered ? 1.0 : 0.0);

        double thumbT = thumbAnim.update();
        double colorT = colorAnim.update();
        double glowT  = glowAnim.update();

        ThemeManager tm = ThemeManager.INSTANCE;
        Color accent    = tm.getAccent();
        Color offColor  = new Color(60, 60, 80, 200);

        // Interpolate track background
        Color trackColor = interpolateColor(offColor, accent, colorT);

        // Glow under the track when hovered
        if (glowT > 0.01) {
            Color glow = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int) (50 * glowT));
            RenderUtils.renderRoundedQuad(context, glow,
                    x - 3, y - 3, x + W + 3, y + H + 3, RADIUS + 3, 8);
        }

        // Track
        RenderUtils.renderRoundedQuad(context, trackColor, x, y, x + W, y + H, RADIUS, 10);

        // Thumb
        int thumbX = (int) (x + 3 + thumbT * (W - 6 - THUMB_RADIUS * 2));
        int thumbY = y + (H - THUMB_RADIUS * 2) / 2;
        Color thumbColor = new Color(235, 235, 240, 255);
        RenderUtils.renderCircle(context, thumbColor,
                thumbX + THUMB_RADIUS, thumbY + THUMB_RADIUS, THUMB_RADIUS, 20);

        // Thumb shadow
        Color thumbShadow = new Color(0, 0, 0, 40);
        RenderUtils.renderCircle(context, thumbShadow,
                thumbX + THUMB_RADIUS, thumbY + THUMB_RADIUS + 1, THUMB_RADIUS - 1, 16);
    }

    /**
     * Call on left-click.  Returns true if this toggle was clicked.
     * Does NOT toggle the setting — the caller should toggle the backing BooleanSetting.
     */
    public boolean isClicked(double mouseX, double mouseY, int button, int x, int y) {
        return button == 0
                && mouseX >= x && mouseX <= x + W
                && mouseY >= y && mouseY <= y + H;
    }

    public boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + W
                && mouseY >= y && mouseY <= y + H;
    }

    private static Color interpolateColor(Color from, Color to, double t) {
        int r = (int) (from.getRed()   + (to.getRed()   - from.getRed())   * t);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        int b = (int) (from.getBlue()  + (to.getBlue()  - from.getBlue())  * t);
        int a = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t);
        return new Color(r, g, b, a);
    }
}
