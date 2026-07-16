package dev.lvstrng.argon.gui.components.settings;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.MathUtils;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * PremiumSlider — animated slider with glow handle and smooth value lerp.
 */
public final class PremiumSlider extends PremiumRenderableSetting {

    private static final int TRACK_H  = 4;
    private static final int THUMB_R  = 7;
    private static final int RADIUS   = 2;

    private final NumberSetting setting;
    private final AnimationManager fillAnim;
    private final AnimationManager thumbScaleAnim;
    private boolean dragging = false;

    public PremiumSlider(NumberSetting setting) {
        this.setting = setting;
        double initial = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        fillAnim      = new AnimationManager(initial, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
        thumbScaleAnim = new AnimationManager(1.0,    AnimationManager.Easing.EASE_OUT_BACK,  14.0);
    }

    @Override
    public void render(DrawContext context,
                       int rx, int ry, int rw, int rh,
                       int mouseX, int mouseY, double alpha) {
        ThemeManager tm = ThemeManager.INSTANCE;

        double fraction = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        fillAnim.setTarget(fraction);
        double fillT = fillAnim.update();

        // Track area (right half of the row)
        int labelW  = rw / 2;
        int sliderX = rx + labelW;
        int sliderW = rw - labelW - 8;
        int trackY  = ry + (rh - TRACK_H) / 2;

        // Label + value
        String label = setting.getName().toString();
        String value = formatValue(setting.getValue());
        TextRenderer.drawString(label, context, rx + 4, ry + rh / 2 - 5,
                tm.getTextSecondary().getRGB());
        TextRenderer.drawString(value, context,
                sliderX - TextRenderer.getWidth(value) - 8,
                ry + rh / 2 - 5, tm.getTextMuted().getRGB());

        // Track bg
        Color trackBg = new Color(255, 255, 255, 25);
        RenderUtils.renderRoundedQuad(context, trackBg,
                sliderX, trackY, sliderX + sliderW, trackY + TRACK_H, RADIUS, 6);

        // Fill
        int fillW = (int)(fillT * sliderW);
        if (fillW > 0) {
            Color accent = tm.getAccent();
            RenderUtils.renderRoundedQuad(context, accent,
                    sliderX, trackY, sliderX + fillW, trackY + TRACK_H, RADIUS, 6);
        }

        // Thumb
        boolean hovered = isThumbHovered(mouseX, mouseY, sliderX, trackY, fillW);
        thumbScaleAnim.setTarget(dragging ? 1.3 : (hovered ? 1.15 : 1.0));
        double scale = thumbScaleAnim.update();

        int thumbX = sliderX + fillW;
        int thumbY = trackY + TRACK_H / 2;
        int r      = (int)(THUMB_R * scale);

        Color accent = tm.getAccent();
        Color glow   = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60);
        RenderUtils.renderCircle(context, glow, thumbX, thumbY, r + 4, 16);
        RenderUtils.renderCircle(context, accent, thumbX, thumbY, r, 16);
        RenderUtils.renderCircle(context, new Color(240, 240, 245), thumbX, thumbY, r - 3, 12);
    }

    private String formatValue(double v) {
        if (v == Math.floor(v)) return String.valueOf((int)v);
        return String.format("%.2f", v);
    }

    private boolean isThumbHovered(int mx, int my, int sliderX, int trackY, int fillW) {
        int thumbX = sliderX + fillW;
        int thumbY = trackY + TRACK_H / 2;
        int dx = mx - thumbX, dy = my - thumbY;
        return dx * dx + dy * dy <= (THUMB_R + 6) * (THUMB_R + 6);
    }

    private void applySlide(double mouseX, int rx, int rw) {
        int labelW  = rw / 2;
        int sliderX = rx + labelW;
        int sliderW = rw - labelW - 8;
        double t = MathHelper.clamp((mouseX - sliderX) / sliderW, 0.0, 1.0);
        setting.setValue(MathUtils.roundToDecimal(t * (setting.getMax() - setting.getMin()) + setting.getMin(),
                setting.getIncrement()));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button,
                             int rx, int ry, int rw, int rh) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        int labelW  = rw / 2;
        int sliderX = rx + labelW;
        int trackY  = ry + (rh - TRACK_H) / 2;
        if (mouseX >= sliderX && mouseX <= sliderX + rw - labelW - 8
                && mouseY >= trackY - 6 && mouseY <= trackY + TRACK_H + 6) {
            dragging = true;
            applySlide(mouseX, rx, rw);
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button,
                             double dX, double dY,
                             int rx, int ry, int rw, int rh) {
        if (dragging) applySlide(mouseX, rx, rw);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) dragging = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE)
            setting.setValue(setting.getOriginalValue());
    }

    @Override
    public boolean matchesQuery(String query) {
        return setting.getName().toString().toLowerCase().contains(query);
    }
}
