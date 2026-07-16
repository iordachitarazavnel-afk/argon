package dev.lvstrng.argon.gui;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.utils.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

/**
 * ScrollHandler — inertia scrolling with mouse-wheel acceleration.
 *
 * Create one instance per scrollable region.  Each frame call update(),
 * then use getScroll() as the vertical offset for your content.
 */
public final class ScrollHandler {

    private double velocity    = 0.0;
    private double scroll      = 0.0;
    private double targetScroll = 0.0;

    private double maxScroll   = 0.0;          // set externally each frame
    private double friction    = 0.88;         // per-frame velocity multiplier
    private double acceleration = 22.0;        // scroll units per wheel tick

    private final AnimationManager smoothScroll;

    /** Width of the visible scrollbar thumb. */
    private static final int SCROLLBAR_W     = 4;
    private static final int SCROLLBAR_RADIUS = 2;

    public ScrollHandler() {
        smoothScroll = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 14.0);
    }

    /* ── Input ───────────────────────────────────────────────────── */

    /**
     * Call on mouse-wheel event.
     * @param amount  positive = scroll down, negative = up
     */
    public void onScroll(double amount) {
        velocity += amount * acceleration;
    }

    /* ── Update ──────────────────────────────────────────────────── */

    /** Advance physics.  Call every frame. */
    public void update() {
        targetScroll += velocity;
        targetScroll  = MathHelper.clamp(targetScroll, 0.0, Math.max(0.0, maxScroll));
        velocity     *= friction;
        if (Math.abs(velocity) < 0.5) velocity = 0;

        smoothScroll.setTarget(targetScroll);
        scroll = smoothScroll.update();
    }

    /** @return current interpolated scroll offset in pixels. */
    public double getScroll() {
        return scroll;
    }

    /** Set total content height minus visible height. */
    public void setMaxScroll(double max) {
        this.maxScroll = Math.max(0.0, max);
    }

    public void reset() {
        scroll       = 0.0;
        targetScroll = 0.0;
        velocity     = 0.0;
        smoothScroll.snap(0.0);
    }

    /* ── Scrollbar rendering ──────────────────────────────────────── */

    /**
     * Render a thin scrollbar on the right edge of [x, y, x+w, y+h].
     * Only visible when scrollable content exists.
     */
    public void renderScrollbar(DrawContext context, int x, int y, int w, int h) {
        if (maxScroll <= 0) return;

        double totalH   = h + maxScroll;          // total content height
        double thumbH   = Math.max(24, h * h / totalH);
        double thumbY   = (scroll / maxScroll) * (h - thumbH);

        ThemeManager tm = ThemeManager.INSTANCE;
        Color track  = new Color(tm.getTextMuted().getRed(),
                                 tm.getTextMuted().getGreen(),
                                 tm.getTextMuted().getBlue(), 40);
        Color thumb  = new Color(tm.getTextSecondary().getRed(),
                                 tm.getTextSecondary().getGreen(),
                                 tm.getTextSecondary().getBlue(), 130);

        int sx = x + w - SCROLLBAR_W - 4;

        // Track
        RenderUtils.renderRoundedQuad(context, track,
                sx, y, sx + SCROLLBAR_W, y + h, SCROLLBAR_RADIUS, 5);

        // Thumb
        RenderUtils.renderRoundedQuad(context, thumb,
                sx, (int) (y + thumbY), sx + SCROLLBAR_W, (int) (y + thumbY + thumbH),
                SCROLLBAR_RADIUS, 5);
    }

    /* ── Scissor helpers ──────────────────────────────────────────── */

    public void beginScissor(int x, int y, int w, int h) {
        RenderUtils.setScissorRegion(x, y, x + w, y + h);
    }

    public void endScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }
}
