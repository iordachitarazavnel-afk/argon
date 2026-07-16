package dev.lvstrng.argon.gui;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sidebar — left navigation panel with category icons, profile section, and theme icon.
 *
 * Width: SIDEBAR_W px (fixed).
 * Categories listed vertically; selected item has an animated left-accent bar + highlight.
 * Bottom section: username, ping, settings gear icon.
 */
public final class Sidebar {

    public static final int SIDEBAR_W = 170;

    private static final int ITEM_H    = 44;
    private static final int RADIUS    = 12;
    private static final int ICON_SIZE = 20;

    /* ── Category icon map (single Unicode char per category) ─────── */
    private static final Map<Category, String> ICONS = new LinkedHashMap<>();
    static {
        ICONS.put(Category.COMBAT, "⚔");
        ICONS.put(Category.RENDER, "◈");
        ICONS.put(Category.MISC,   "✦");
        ICONS.put(Category.CLIENT, "⚙");
    }

    /* ── Extra sidebar entries ────────────────────────────────────── */
    public enum SidebarSection { CATEGORY, THEME, FRIENDS }

    /* ── State ────────────────────────────────────────────────────── */

    private Category selected = Category.COMBAT;
    private SidebarSection hoveredSection = null;

    // Per-category hover and selection animation
    private final Map<Category, AnimationManager> hoverAnims   = new LinkedHashMap<>();
    private final Map<Category, AnimationManager> selectAnims  = new LinkedHashMap<>();

    private final AnimationManager themeHoverAnim   = AnimationManager.forFade();
    private final AnimationManager friendsHoverAnim = AnimationManager.forFade();

    private boolean themeHovered   = false;
    private boolean friendsHovered = false;

    public Sidebar() {
        for (Category cat : Category.values()) {
            hoverAnims.put(cat,  new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0));
            selectAnims.put(cat, new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 10.0));
        }
    }

    /* ── Render ──────────────────────────────────────────────────── */

    /**
     * Render the sidebar.
     *
     * @param x, y    top-left corner (after GUI margin)
     * @param h       full panel height
     * @param mouseX, mouseY  current mouse position
     */
    public void render(DrawContext context, int x, int y, int h,
                       int mouseX, int mouseY, float delta) {
        ThemeManager tm = ThemeManager.INSTANCE;

        // Sidebar background
        Color sbBg = tm.getSidebarBg();
        RenderUtils.renderRoundedQuad(context, sbBg,
                x, y, x + SIDEBAR_W, y + h,
                RADIUS, 0, RADIUS, 0, 10);

        // Separator line on the right
        Color sep = new Color(255, 255, 255, 18);
        context.fill(x + SIDEBAR_W - 1, y + 8, x + SIDEBAR_W, y + h - 8, sep.getRGB());

        // ── Client title ──────────────────────────────────────────
        Color titleColor = tm.getAccentBright();
        TextRenderer.drawString("ARGON", context, x + 18, y + 18, titleColor.getRGB());

        // ── Category items ────────────────────────────────────────
        int itemY = y + 56;
        for (Category cat : Category.values()) {
            renderCategoryItem(context, x, itemY, cat, mouseX, mouseY);
            itemY += ITEM_H + 4;
        }

        // ── Bottom: Friends + Theme + Profile ─────────────────────
        int bottomY = y + h - 120;

        // Divider
        Color div = new Color(255, 255, 255, 15);
        context.fill(x + 12, bottomY, x + SIDEBAR_W - 12, bottomY + 1, div.getRGB());

        // Friends button
        renderBottomButton(context, x, bottomY + 10, mouseX, mouseY,
                "Friends", "♣", friendsHoverAnim, friendsHovered);

        // Theme button
        renderBottomButton(context, x, bottomY + 54, mouseX, mouseY,
                "Theme", "◐", themeHoverAnim, themeHovered);

        // Profile section
        renderProfile(context, x, y + h - 44, mouseX, mouseY);
    }

    private void renderCategoryItem(DrawContext context, int x, int y,
                                    Category cat, int mouseX, int mouseY) {
        ThemeManager tm = ThemeManager.INSTANCE;
        boolean isHovered  = isItemHovered(x, y, mouseX, mouseY);
        boolean isSelected = selected == cat;

        AnimationManager hAnim = hoverAnims.get(cat);
        AnimationManager sAnim = selectAnims.get(cat);
        hAnim.setTarget(isHovered  ? 1.0 : 0.0);
        sAnim.setTarget(isSelected ? 1.0 : 0.0);

        double hT = hAnim.update();
        double sT = sAnim.update();

        // Item background
        if (hT > 0.01 || sT > 0.01) {
            Color accent = tm.getAccent();
            Color hBg    = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int) (Math.max(hT * 20, sT * 35)));
            RenderUtils.renderRoundedQuad(context, hBg,
                    x + 8, y, x + SIDEBAR_W - 8, y + ITEM_H, 10, 8);
        }

        // Animated left accent bar for selected item
        if (sT > 0.01) {
            Color accent  = tm.getAccent();
            int barH      = (int) (ITEM_H * 0.6 * sT);
            int barY      = y + (ITEM_H - barH) / 2;
            RenderUtils.renderRoundedQuad(context, accent,
                    x + 8, barY, x + 11, barY + barH, 1, 6);
        }

        // Icon
        String icon = ICONS.getOrDefault(cat, "•");
        Color iconColor = sT > 0.3
                ? tm.getAccentBright()
                : (hT > 0.3 ? tm.getTextPrimary() : tm.getTextSecondary());
        TextRenderer.drawString(icon, context, x + 20, y + (ITEM_H - 12) / 2, iconColor.getRGB());

        // Label
        String label = cat.name.toString();
        TextRenderer.drawString(label, context, x + 40, y + (ITEM_H - 10) / 2, iconColor.getRGB());
    }

    private void renderBottomButton(DrawContext context, int x, int y,
                                    int mouseX, int mouseY,
                                    String label, String icon,
                                    AnimationManager anim, boolean hovered) {
        ThemeManager tm = ThemeManager.INSTANCE;
        anim.setTarget(hovered ? 1.0 : 0.0);
        double t = anim.update();

        if (t > 0.01) {
            Color bg = new Color(255, 255, 255, (int)(20 * t));
            RenderUtils.renderRoundedQuad(context, bg, x + 8, y, x + SIDEBAR_W - 8, y + ITEM_H, 8, 6);
        }

        Color c = hovered ? tm.getTextPrimary() : tm.getTextSecondary();
        TextRenderer.drawString(icon,  context, x + 20, y + (ITEM_H - 10) / 2, c.getRGB());
        TextRenderer.drawString(label, context, x + 40, y + (ITEM_H - 10) / 2, c.getRGB());
    }

    private void renderProfile(DrawContext context, int x, int y, int mouseX, int mouseY) {
        ThemeManager tm = ThemeManager.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "Player";

        // Avatar circle
        Color accent = tm.getAccent();
        RenderUtils.renderCircle(context, accent, x + 28, y + 20, 14, 20);
        Color avatarBg = new Color(30, 30, 50, 255);
        RenderUtils.renderCircle(context, avatarBg, x + 28, y + 20, 12, 20);

        // Initial letter
        String initial = name.substring(0, 1).toUpperCase();
        TextRenderer.drawString(initial, context, x + 23, y + 13, tm.getTextPrimary().getRGB());

        // Username + status dot
        TextRenderer.drawString(name, context, x + 46, y + 10, tm.getTextPrimary().getRGB());
        TextRenderer.drawString("Online",  context, x + 46, y + 24, tm.getTextMuted().getRGB());

        // Green status dot
        Color green = new Color(0x3D, 0xD6, 0x8C);
        RenderUtils.renderCircle(context, green, x + 44, y + 25, 3, 8);
    }

    /* ── Mouse events ────────────────────────────────────────────── */

    public void mouseClicked(double mouseX, double mouseY, int button,
                             int x, int y, int h) {
        if (button != 0) return;

        int itemY = y + 56;
        for (Category cat : Category.values()) {
            if (isItemHovered(x, itemY, (int) mouseX, (int) mouseY)) {
                selected = cat;
                return;
            }
            itemY += ITEM_H + 4;
        }
    }

    public void mouseMoved(double mouseX, double mouseY, int x, int y, int h) {
        themeHovered   = isInBottomButton(mouseX, mouseY, x, y + h - 120 + 54);
        friendsHovered = isInBottomButton(mouseX, mouseY, x, y + h - 120 + 10);
    }

    /** Returns true if the friends button was clicked (for popup trigger). */
    public boolean isFriendsClicked(double mouseX, double mouseY, int x, int y, int h) {
        return isInBottomButton(mouseX, mouseY, x, y + h - 120 + 10);
    }

    /** Returns true if the theme button was clicked. */
    public boolean isThemeClicked(double mouseX, double mouseY, int x, int y, int h) {
        return isInBottomButton(mouseX, mouseY, x, y + h - 120 + 54);
    }

    /* ── Helpers ─────────────────────────────────────────────────── */

    private boolean isItemHovered(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x + 8 && mouseX <= x + SIDEBAR_W - 8
                && mouseY >= y && mouseY <= y + ITEM_H;
    }

    private boolean isInBottomButton(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x + 8 && mouseX <= x + SIDEBAR_W - 8
                && mouseY >= y && mouseY <= y + ITEM_H;
    }

    public Category getSelected() { return selected; }
}
