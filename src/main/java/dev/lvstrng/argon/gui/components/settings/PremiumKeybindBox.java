package dev.lvstrng.argon.gui.components.settings;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.module.setting.KeybindSetting;
import dev.lvstrng.argon.utils.KeyUtils;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * PremiumKeybindBox — keybind selector with animated listening pill.
 */
public final class PremiumKeybindBox extends PremiumRenderableSetting {

    private final KeybindSetting setting;
    private final AnimationManager blinkAnim;
    private long listenStart = 0;

    public PremiumKeybindBox(KeybindSetting setting) {
        this.setting   = setting;
        this.blinkAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_IN_OUT_CUBIC, 3.0);
    }

    @Override
    public void render(DrawContext context,
                       int rx, int ry, int rw, int rh,
                       int mouseX, int mouseY, double alpha) {
        ThemeManager tm = ThemeManager.INSTANCE;

        // Label
        TextRenderer.drawString(setting.getName().toString(), context,
                rx + 4, ry + rh / 2 - 5, tm.getTextSecondary().getRGB());

        // Keybind pill
       // Keybind pill
String keyText = setting.isListening()
        ? "Press key..."
        : KeyUtils.getKey(setting.getKey().toString());  // ← Adaugă .toString()
        
        int pillW = TextRenderer.getWidth(keyText) + 16;
        int pillX = rx + rw - pillW - 4;
        int pillY = ry + (rh - 20) / 2;

        Color pillBg;
        if (setting.isListening()) {
            // Pulsing accent
            boolean pulse = ((System.currentTimeMillis() - listenStart) % 1000) < 500;
            Color accent  = tm.getAccent();
            pillBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), pulse ? 80 : 40);
        } else {
            pillBg = new Color(255, 255, 255, 20);
        }

        RenderUtils.renderRoundedQuad(context, pillBg, pillX, pillY, pillX + pillW, pillY + 20, 6, 8);
        RenderUtils.renderRoundedOutline(context, new Color(255, 255, 255, 35),
                pillX, pillY, pillX + pillW, pillY + 20, 6, 6, 6, 6, 1, 6);

        Color textColor = setting.isListening() ? tm.getAccentBright() : tm.getTextPrimary();
        TextRenderer.drawString(keyText, context, pillX + 8, pillY + 5, textColor.getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button,
                             int rx, int ry, int rw, int rh) {
        if (!isHovered(mouseX, mouseY, rx, ry, rw, rh)) return;

        if (!setting.isListening()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                setting.setListening(true);
                listenStart = System.currentTimeMillis();
            }
        } else {
            if (setting.isModuleKey()) {
                // Handled in keyPressed
            }
            setting.setKey(button);
            setting.setListening(false);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!setting.isListening()) return;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setting.setListening(false);
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            setting.setKey(setting.getOriginalKey());
            setting.setListening(false);
            return;
        }
        if (setting.isModuleKey()) {
            // parent module key set is handled by the module card via getModule().setKey()
        }
        setting.setKey(keyCode);
        setting.setListening(false);
    }

    @Override
    public boolean matchesQuery(String query) {
        return setting.getName().toString().toLowerCase().contains(query);
    }

    private boolean isHovered(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }
}
