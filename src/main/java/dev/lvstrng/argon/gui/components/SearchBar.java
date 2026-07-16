package dev.lvstrng.argon.gui.components;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * SearchBar — floating animated search bar at the top of the GUI.
 *
 * Features:
 *  • Animated expand/collapse on focus
 *  • Border glow when focused
 *  • Cursor blink
 *  • Backspace / Ctrl+A / character input
 *  • Returns the current query via getQuery() for instant filtering
 */
public final class SearchBar {

    private static final int HEIGHT = 32;
    private static final int RADIUS = 16;

    private final StringBuilder query = new StringBuilder();
    private boolean focused = false;
    private long   cursorTime = 0;

    private final AnimationManager focusAnim;
    private final AnimationManager glowAnim;

    public SearchBar() {
        focusAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
        glowAnim  = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_CUBIC, 8.0);
    }

    /* ── Render ──────────────────────────────────────────────────── */

    /**
     * Render the search bar.
     *
     * @param context DrawContext
     * @param cx      center X of the bar
     * @param y       top Y
     * @param maxW    maximum width when focused
     */
    public void render(DrawContext context, int cx, int y, int maxW) {
        focusAnim.setTarget(focused ? 1.0 : 0.0);
        glowAnim.setTarget(focused ? 1.0 : 0.0);

        double focusT = focusAnim.update();
        double glowT  = glowAnim.update();

        int w = (int) (180 + (maxW - 180) * focusT);
        int x = cx - w / 2;

        ThemeManager tm = ThemeManager.INSTANCE;
        Color bg = new Color(tm.getPanelBg().getRed(), tm.getPanelBg().getGreen(),
                             tm.getPanelBg().getBlue(), (int)(210 * Math.max(0.6, focusT + 0.6)));

        // Glow
        if (glowT > 0.01) {
            Color accent = tm.getAccent();
            Color glow   = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int) (55 * glowT));
            RenderUtils.renderRoundedQuad(context, glow,
                    x - 4, y - 4, x + w + 4, y + HEIGHT + 4, RADIUS + 4, 8);
        }

        // Background
        RenderUtils.renderRoundedQuad(context, bg, x, y, x + w, y + HEIGHT, RADIUS, 10);

        // Border
        Color border = focused
                ? new Color(tm.getAccent().getRed(), tm.getAccent().getGreen(), tm.getAccent().getBlue(),
                            (int)(180 * glowT))
                : new Color(255, 255, 255, 30);
        RenderUtils.renderRoundedOutline(context, border, x, y, x + w, y + HEIGHT,
                RADIUS, RADIUS, RADIUS, RADIUS, 1, 10);

        // Placeholder / query text
        int textX   = x + 14;
        int textY   = y + HEIGHT / 2 - 6;
        Color textC = tm.getTextPrimary();

        if (query.isEmpty() && !focused) {
            Color muted = tm.getTextMuted();
            TextRenderer.drawString("Search modules...", context, textX, textY, muted.getRGB());
        } else {
            String display = query.toString();
            TextRenderer.drawString(display, context, textX, textY, textC.getRGB());

            // Blinking cursor
            if (focused) {
                long now   = System.currentTimeMillis();
                boolean on = ((now - cursorTime) % 1000) < 500;
                if (on) {
                    int cursorX = textX + TextRenderer.getWidth(display) + 1;
                    context.fill(cursorX, textY, cursorX + 1, textY + 12, textC.getRGB());
                }
            }
        }

        // Search icon (left side, simple "○—" using fill)
        renderSearchIcon(context, x + w - 20, y + HEIGHT / 2, tm.getTextMuted());
    }

    private void renderSearchIcon(DrawContext context, int cx, int cy, Color color) {
        // Simple circle + line representing a magnifier
        RenderUtils.renderCircle(context, color, cx, cy, 5, 12);
        Color bg = ThemeManager.INSTANCE.getPanelBg();
        RenderUtils.renderCircle(context, bg, cx, cy, 3, 12);
        // Line (handle)
        context.fill(cx + 4, cy + 4, cx + 7, cy + 7, color.getRGB());
    }

    /* ── Input ───────────────────────────────────────────────────── */

    public void mouseClicked(double mouseX, double mouseY, int button, int cx, int y, int maxW) {
        double focusT = focusAnim.getValue();
        int w = (int) (180 + (maxW - 180) * focusT);
        int x = cx - w / 2;

        boolean inside = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + HEIGHT;

        if (inside && button == 0) {
            if (!focused) {
                focused = true;
                cursorTime = System.currentTimeMillis();
            }
        } else if (!inside) {
            focused = false;
        }
    }

    public void charTyped(char chr) {
        if (!focused) return;
        if (chr >= 32 && chr < 127) {           // printable ASCII
            query.append(chr);
        }
    }

    public void keyPressed(int keyCode, int modifiers) {
        if (!focused) return;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (ctrl) {
                query.setLength(0);
            } else if (!query.isEmpty()) {
                query.deleteCharAt(query.length() - 1);
            }
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!query.isEmpty()) {
                query.setLength(0);
            } else {
                focused = false;
            }
        } else if (keyCode == GLFW.GLFW_KEY_A && ctrl) {
            query.setLength(0);
        }
    }

    /* ── Accessors ───────────────────────────────────────────────── */

    public String getQuery() {
        return query.toString().toLowerCase();
    }

    public boolean isFocused() {
        return focused;
    }

    public void clear() {
        query.setLength(0);
        focused = false;
    }

    /** Height constant for layout calculations. */
    public static int getHeight() {
        return HEIGHT;
    }
}
