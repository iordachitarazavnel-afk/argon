package dev.lvstrng.argon.gui;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * RoundedPanel — renders a glassmorphism-style panel.
 *
 * Features:
 *  • Rounded corners (configurable radius)
 *  • Optional hover glow on the border
 *  • Animated alpha fade-in/out
 *  • Drop-shadow via layered offset quads
 */
public final class RoundedPanel {

    private final int radius;
    private final AnimationManager alphaAnim;
    private final AnimationManager glowAnim;

    public RoundedPanel(int radius) {
        this.radius   = radius;
        this.alphaAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_CUBIC, 8.0);
        this.glowAnim  = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
    }

    /**
     * Render the panel.
     *
     * @param context   DrawContext
     * @param x, y      top-left
     * @param w, h      width / height
     * @param hovered   whether the mouse is inside (drives glow)
     * @param visible   whether the panel should be visible (drives alpha)
     * @param color     base fill color (alpha used as max-alpha target)
     */
    public void render(DrawContext context, int x, int y, int w, int h,
                       boolean hovered, boolean visible, Color color) {
        alphaAnim.setTarget(visible ? 1.0 : 0.0);
        glowAnim.setTarget(hovered ? 1.0 : 0.0);

        double alpha = alphaAnim.update();
        double glow  = glowAnim.update();

        if (alpha <= 0.01) return;

        int a = (int) (color.getAlpha() * alpha);

        /* ── Shadow (layered offset fill) ─────────────────────────── */
        renderShadow(context, x, y, w, h, a);

        /* ── Background ───────────────────────────────────────────── */
        Color bg = new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
        RenderUtils.renderRoundedQuad(context, bg, x, y, x + w, y + h, radius, 10);

        /* ── Border ───────────────────────────────────────────────── */
        ThemeManager tm  = ThemeManager.INSTANCE;
        Color accentC    = tm.getAccent();
        // Base border
        Color borderBase = new Color(255, 255, 255, (int) (30 * alpha));
        RenderUtils.renderRoundedOutline(context, borderBase, x, y, x + w, y + h,
                radius, radius, radius, radius, 1, 10);

        if (glow > 0.01) {
            // Accent glow border
            Color glowColor = new Color(accentC.getRed(), accentC.getGreen(), accentC.getBlue(),
                    (int) (70 * glow * alpha));
            RenderUtils.renderRoundedOutline(context, glowColor, x, y, x + w, y + h,
                    radius, radius, radius, radius, 1, 10);
        }
    }

    /** Render only the background (no glow state). */
    public void renderStatic(DrawContext context, int x, int y, int w, int h, Color color) {
        renderShadow(context, x, y, w, h, color.getAlpha());
        RenderUtils.renderRoundedQuad(context, color, x, y, x + w, y + h, radius, 10);
        Color border = new Color(255, 255, 255, 25);
        RenderUtils.renderRoundedOutline(context, border, x, y, x + w, y + h,
                radius, radius, radius, radius, 1, 10);
    }

    private void renderShadow(DrawContext context, int x, int y, int w, int h, int baseAlpha) {
        // 3-layer soft shadow
        for (int i = 3; i >= 1; i--) {
            int off   = i * 3;
            int sAlpha = (int) (baseAlpha * 0.08 * (4 - i));
            Color shadow = new Color(0, 0, 0, sAlpha);
            RenderUtils.renderRoundedQuad(context, shadow,
                    x - off, y + off, x + w + off, y + h + off, radius + off, 8);
        }
    }

    public boolean isVisible() {
        return alphaAnim.getValue() > 0.01;
    }
}
