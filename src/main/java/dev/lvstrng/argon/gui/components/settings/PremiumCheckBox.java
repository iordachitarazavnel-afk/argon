package dev.lvstrng.argon.gui.components.settings;

import dev.lvstrng.argon.gui.components.ToggleSwitch;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * PremiumCheckBox — boolean setting rendered as an inline toggle switch.
 */
public final class PremiumCheckBox extends PremiumRenderableSetting {

    private final BooleanSetting setting;
    private final ToggleSwitch toggle = new ToggleSwitch();

    public PremiumCheckBox(BooleanSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context,
                       int rx, int ry, int rw, int rh,
                       int mouseX, int mouseY, double alpha) {
        ThemeManager tm = ThemeManager.INSTANCE;

        // Label on the left
        int textY = ry + rh / 2 - 5;
        TextRenderer.drawString(setting.getName().toString(), context, rx + 4, textY,
                tm.getTextSecondary().getRGB());

        // Toggle on the right
        int tX = rx + rw - ToggleSwitch.W - 4;
        int tY = ry + (rh - ToggleSwitch.H) / 2;
        boolean hovered = toggle.isHovered(mouseX, mouseY, tX, tY);
        toggle.render(context, tX, tY, setting.getValue(), hovered);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button,
                             int rx, int ry, int rw, int rh) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        int tX = rx + rw - ToggleSwitch.W - 4;
        int tY = ry + (rh - ToggleSwitch.H) / 2;
        if (toggle.isClicked(mouseX, mouseY, button, tX, tY)) {
            setting.toggle();
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            setting.setValue(setting.getOriginalValue());
        }
    }

    @Override
    public boolean matchesQuery(String query) {
        return setting.getName().toString().toLowerCase().contains(query);
    }
}
