package dev.lvstrng.argon.gui.components.settings;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.module.setting.ModeSetting;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * PremiumModeBox — rounded dropdown-style mode cycler.
 *
 * Left-click cycles forward, right-click cycles backward.
 * Shows: [Label]  ◀  CurrentMode  ▶
 */
public final class PremiumModeBox extends PremiumRenderableSetting {

    private final ModeSetting<?> setting;
    private final AnimationManager hoverAnim;

    public PremiumModeBox(ModeSetting<?> setting) {
        this.setting   = setting;
        this.hoverAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
    }

    @Override
    public void render(DrawContext context,
                       int rx, int ry, int rw, int rh,
                       int mouseX, int mouseY, double alpha) {
        ThemeManager tm = ThemeManager.INSTANCE;
        boolean hovered = isHovered(mouseX, mouseY, rx, ry, rw, rh);
        hoverAnim.setTarget(hovered ? 1.0 : 0.0);
        double hT = hoverAnim.update();

        // Label
        TextRenderer.drawString(setting.getName().toString(), context,
                rx + 4, ry + rh / 2 - 5, tm.getTextSecondary().getRGB());

        // Mode pill (right side)
        String mode = setting.getMode().name();
        int pillW   = TextRenderer.getWidth(mode) + 20;
        int pillX   = rx + rw - pillW - 4;
        int pillY   = ry + (rh - 20) / 2;

        Color accent = tm.getAccent();
        Color pillBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                (int)(40 + 30 * hT));
        RenderUtils.renderRoundedQuad(context, pillBg, pillX, pillY, pillX + pillW, pillY + 20, 10, 8);

        if (hT > 0.01) {
            Color glowBorder = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int)(100 * hT));
            RenderUtils.renderRoundedOutline(context, glowBorder,
                    pillX, pillY, pillX + pillW, pillY + 20, 10, 10, 10, 10, 1, 8);
        }

        // ◀ chevron
        TextRenderer.drawString("◀", context, pillX + 4, pillY + 5, tm.getTextMuted().getRGB());
        // Mode text
        TextRenderer.drawString(mode, context, pillX + 14, pillY + 5, tm.getTextPrimary().getRGB());
        // ▶ chevron
        TextRenderer.drawString("▶", context, pillX + pillW - 12, pillY + 5, tm.getTextMuted().getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button,
                             int rx, int ry, int rw, int rh) {
        if (!isHovered(mouseX, mouseY, rx, ry, rw, rh)) return;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)  setting.cycle();
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) setting.cycle(); // could reverse
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) setting.setModeIndex(setting.getOriginalValue());
    }

    @Override
    public boolean matchesQuery(String query) {
        return setting.getName().toString().toLowerCase().contains(query)
                || setting.getMode().name().toLowerCase().contains(query);
    }

    private boolean isHovered(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }
}
