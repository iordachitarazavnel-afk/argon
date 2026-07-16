package dev.lvstrng.argon.gui;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FriendsPopup — draggable floating friends panel.
 *
 * Shows an in-memory friend list (integrate with your own API as needed).
 * Supports: search, add, remove, favorite, online indicator.
 */
public final class FriendsPopup {

    /* ── Friend record ───────────────────────────────────────────── */

    public static final class Friend {
        public final String name;
        public boolean online;
        public boolean favorite;

        public Friend(String name, boolean online, boolean favorite) {
            this.name     = name;
            this.online   = online;
            this.favorite = favorite;
        }
    }

    /* ── Dimensions ──────────────────────────────────────────────── */

    private static final int W      = 260;
    private static final int H      = 360;
    private static final int RADIUS = 14;
    private static final int ROW_H  = 42;

    /* ── State ────────────────────────────────────────────────────── */

    private boolean visible = false;
    private int x, y;

    // Dragging
    private boolean dragging = false;
    private int     dragOffX, dragOffY;

    // Search
    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused = false;

    // Friends list (demo data — replace with real data source)
    private final List<Friend> friends = new ArrayList<>();
    private final ScrollHandler scroll = new ScrollHandler();

    // Animations
    private final AnimationManager showAnim;

    public FriendsPopup() {
        showAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_BACK, 12.0);

        // Demo friends
        friends.add(new Friend("Notch",       false, true));
        friends.add(new Friend("jeb_",        true,  false));
        friends.add(new Friend("Dinnerbone",  true,  false));
        friends.add(new Friend("Dream",       false, false));
        friends.add(new Friend("Technoblade", false, true));
    }

    /* ── Visibility ──────────────────────────────────────────────── */

    public void toggle(int anchorX, int anchorY) {
        visible = !visible;
        if (visible) {
            x = anchorX;
            y = anchorY;
        }
        showAnim.setTarget(visible ? 1.0 : 0.0);
    }

    public boolean isVisible() { return visible || showAnim.getValue() > 0.01; }

    /* ── Render ──────────────────────────────────────────────────── */

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        double t = showAnim.update();
        if (t <= 0.01) return;

        ThemeManager tm = ThemeManager.INSTANCE;

        // Scale pivot = top-left corner
        double scale = 0.85 + 0.15 * t;
        int alpha    = (int)(220 * t);

        // Shadow
        for (int i = 3; i >= 1; i--) {
            Color shadow = new Color(0, 0, 0, (int)(40 * t * (4 - i) / 3.0));
            RenderUtils.renderRoundedQuad(context, shadow,
                    x - i * 3, y + i * 3, x + W + i * 3, y + H + i * 3, RADIUS + i * 2, 8);
        }

        // Panel
        Color bg = new Color(tm.getPanelBg().getRed(), tm.getPanelBg().getGreen(),
                             tm.getPanelBg().getBlue(), alpha);
        RenderUtils.renderRoundedQuad(context, bg, x, y, x + W, y + H, RADIUS, 10);

        // Border
        Color border = new Color(255, 255, 255, (int)(30 * t));
        RenderUtils.renderRoundedOutline(context, border, x, y, x + W, y + H,
                RADIUS, RADIUS, RADIUS, RADIUS, 1, 10);

        // Title bar
        renderTitleBar(context, t, tm, mouseX, mouseY);

        // Search bar
        renderSearchBar(context, t, tm, mouseX, mouseY);

        // Friends list
        renderFriendsList(context, t, tm, mouseX, mouseY);

        // Scrollbar
        scroll.update();
        scroll.renderScrollbar(context, x, y + 80, W, H - 80);
    }

    private void renderTitleBar(DrawContext context, double t, ThemeManager tm,
                                int mouseX, int mouseY) {
        int alpha = (int)(255 * t);

        // Title
        Color titleC = new Color(tm.getTextPrimary().getRed(), tm.getTextPrimary().getGreen(),
                                 tm.getTextPrimary().getBlue(), alpha);
        TextRenderer.drawString("Friends", context, x + 16, y + 14, titleC.getRGB());

        // Online count
        long online = friends.stream().filter(f -> f.online).count();
        Color mutedC = new Color(tm.getTextMuted().getRed(), tm.getTextMuted().getGreen(),
                                 tm.getTextMuted().getBlue(), alpha);
        TextRenderer.drawString(online + " online", context, x + 16, y + 28, mutedC.getRGB());

        // Close button
        Color closeC = isCloseHovered(mouseX, mouseY)
                ? new Color(255, 80, 80, alpha)
                : new Color(tm.getTextMuted().getRed(), tm.getTextMuted().getGreen(),
                            tm.getTextMuted().getBlue(), alpha);
        TextRenderer.drawString("✕", context, x + W - 22, y + 14, closeC.getRGB());

        // Separator
        Color sep = new Color(255, 255, 255, (int)(15 * t));
        context.fill(x + 8, y + 42, x + W - 8, y + 43, sep.getRGB());
    }

    private void renderSearchBar(DrawContext context, double t, ThemeManager tm,
                                 int mouseX, int mouseY) {
        int alpha = (int)(200 * t);
        boolean hovered = mouseX >= x + 8 && mouseX <= x + W - 8
                && mouseY >= y + 48 && mouseY <= y + 68;

        Color sbBg = new Color(255, 255, 255, (int)(10 + (hovered || searchFocused ? 10 : 0)));
        RenderUtils.renderRoundedQuad(context, sbBg, x + 8, y + 48, x + W - 8, y + 68, 8, 6);

        Color sbBorder = searchFocused
                ? new Color(tm.getAccent().getRed(), tm.getAccent().getGreen(),
                            tm.getAccent().getBlue(), (int)(150 * t))
                : new Color(255, 255, 255, (int)(25 * t));
        RenderUtils.renderRoundedOutline(context, sbBorder, x + 8, y + 48, x + W - 8, y + 68,
                8, 8, 8, 8, 1, 6);

        Color textC = search.isEmpty()
                ? new Color(tm.getTextMuted().getRed(), tm.getTextMuted().getGreen(),
                            tm.getTextMuted().getBlue(), alpha)
                : new Color(tm.getTextPrimary().getRed(), tm.getTextPrimary().getGreen(),
                            tm.getTextPrimary().getBlue(), alpha);
        String display = search.isEmpty() ? "Search friends..." : search.toString();
        TextRenderer.drawString(display, context, x + 16, y + 54, textC.getRGB());
    }

    private void renderFriendsList(DrawContext context, double t, ThemeManager tm,
                                   int mouseX, int mouseY) {
        String query = search.toString().toLowerCase();
        int listY    = y + 80 - (int) scroll.getScroll();
        int totalH   = 0;

        // Favorites first
        boolean shownFavHeader = false;
        for (Friend f : friends) {
            if (!f.favorite) continue;
            if (!query.isEmpty() && !f.name.toLowerCase().contains(query)) continue;
            if (!shownFavHeader) {
                renderSectionHeader(context, x + 8, listY, t, tm, "Favourites");
                listY += 22; totalH += 22;
                shownFavHeader = true;
            }
            renderFriendRow(context, f, x + 8, listY, W - 16, ROW_H, t, tm, mouseX, mouseY);
            listY += ROW_H + 4; totalH += ROW_H + 4;
        }

        // All others
        boolean shownAllHeader = false;
        for (Friend f : friends) {
            if (f.favorite) continue;
            if (!query.isEmpty() && !f.name.toLowerCase().contains(query)) continue;
            if (!shownAllHeader) {
                renderSectionHeader(context, x + 8, listY, t, tm, "All Friends");
                listY += 22; totalH += 22;
                shownAllHeader = true;
            }
            renderFriendRow(context, f, x + 8, listY, W - 16, ROW_H, t, tm, mouseX, mouseY);
            listY += ROW_H + 4; totalH += ROW_H + 4;
        }

        scroll.setMaxScroll(Math.max(0, totalH - (H - 80)));
    }

    private void renderSectionHeader(DrawContext context, int hx, int hy, double t,
                                     ThemeManager tm, String label) {
        Color c = new Color(tm.getTextMuted().getRed(), tm.getTextMuted().getGreen(),
                            tm.getTextMuted().getBlue(), (int)(180 * t));
        TextRenderer.drawString(label.toUpperCase(), context, hx + 4, hy + 6, c.getRGB());
    }

    private void renderFriendRow(DrawContext context, Friend f, int rx, int ry,
                                 int rw, int rh, double t, ThemeManager tm,
                                 int mouseX, int mouseY) {
        boolean hovered = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rh;
        int alpha = (int)(255 * t);

        if (hovered) {
            Color hBg = new Color(255, 255, 255, (int)(12 * t));
            RenderUtils.renderRoundedQuad(context, hBg, rx, ry, rx + rw, ry + rh, 8, 6);
        }

        // Avatar circle
        Color avBg = new Color(40, 40, 60, alpha);
        RenderUtils.renderCircle(context, avBg, rx + 18, ry + rh / 2, 14, 16);
        Color avText = new Color(tm.getTextSecondary().getRed(),
                                 tm.getTextSecondary().getGreen(),
                                 tm.getTextSecondary().getBlue(), alpha);
        TextRenderer.drawString(f.name.substring(0, 1).toUpperCase(), context,
                rx + 13, ry + rh / 2 - 5, avText.getRGB());

        // Online dot
        Color dotColor = f.online ? new Color(0x3D, 0xD6, 0x8C, alpha) : new Color(100, 100, 120, alpha);
        RenderUtils.renderCircle(context, dotColor, rx + 28, ry + rh / 2 + 6, 4, 8);

        // Name
        Color nameC = new Color(tm.getTextPrimary().getRed(), tm.getTextPrimary().getGreen(),
                                tm.getTextPrimary().getBlue(), alpha);
        TextRenderer.drawString(f.name, context, rx + 36, ry + rh / 2 - 9, nameC.getRGB());

        // Status
        Color statusC = new Color(tm.getTextMuted().getRed(), tm.getTextMuted().getGreen(),
                                  tm.getTextMuted().getBlue(), alpha);
        TextRenderer.drawString(f.online ? "Online" : "Offline", context,
                rx + 36, ry + rh / 2 + 3, statusC.getRGB());

        // Star (favorite)
        Color starC = f.favorite
                ? new Color(0xFF, 0xB3, 0x47, alpha)
                : new Color(tm.getTextMuted().getRed(), tm.getTextMuted().getGreen(),
                            tm.getTextMuted().getBlue(), (int)(100 * t));
        TextRenderer.drawString("★", context, rx + rw - 18, ry + rh / 2 - 5, starC.getRGB());
    }

    /* ── Input ───────────────────────────────────────────────────── */

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) return;

        // Close button
        if (isCloseHovered((int)mouseX, (int)mouseY)) {
            toggle(x, y); return;
        }

        // Title drag
        if (mouseX >= x && mouseX <= x + W && mouseY >= y && mouseY <= y + 42 && button == 0) {
            dragging = true;
            dragOffX = (int)(mouseX - x);
            dragOffY = (int)(mouseY - y);
            return;
        }

        // Search click
        if (mouseX >= x + 8 && mouseX <= x + W - 8 && mouseY >= y + 48 && mouseY <= y + 68) {
            searchFocused = true;
            return;
        }
        searchFocused = false;

        // Friend row clicks (favorite toggle)
        String query = search.toString().toLowerCase();
        int listY = y + 80 - (int) scroll.getScroll();
        for (Friend f : friends) {
            if (!query.isEmpty() && !f.name.toLowerCase().contains(query)) continue;
            int starX = x + 8 + W - 16 - 18;
            if (mouseX >= starX && mouseX <= starX + 18
                    && mouseY >= listY && mouseY <= listY + ROW_H) {
                f.favorite = !f.favorite;
            }
            listY += ROW_H + 4;
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            x = (int)(mouseX - dragOffX);
            y = (int)(mouseY - dragOffY);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
    }

    public void mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isVisible() && mouseX >= x && mouseX <= x + W && mouseY >= y && mouseY <= y + H)
            scroll.onScroll(amount);
    }

    public void charTyped(char chr) {
        if (!searchFocused) return;
        if (chr >= 32 && chr < 127) search.append(chr);
    }

    public void keyPressed(int keyCode) {
        if (!searchFocused) return;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty())
            search.deleteCharAt(search.length() - 1);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) searchFocused = false;
    }

    /* ── Helpers ─────────────────────────────────────────────────── */

    private boolean isCloseHovered(int mx, int my) {
        return mx >= x + W - 28 && mx <= x + W - 8 && my >= y + 8 && my <= y + 30;
    }
}
