package dev.lvstrng.argon.gui.components;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.components.settings.*;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.*;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ModuleCard — a rounded card representing one module.
 *
 * Layout per card:
 *   ┌──────────────────────────────────────────┐
 *   │ [Name]          [description]  [toggle]  │
 *   │ ── settings (animated expand) ──────────  │
 *   └──────────────────────────────────────────┘
 *
 * Height:
 *   collapsed: CARD_H
 *   expanded:  CARD_H + settingRows * SETTING_H + SETTINGS_PAD
 */
public final class ModuleCard {

    public static final int CARD_H     = 48;
    public static final int SETTING_H  = 30;
    public static final int SETTINGS_PAD = 8;
    public static final int RADIUS     = 12;
    public static final int MARGIN     = 6;

    private final Module module;
    private final ToggleSwitch toggle = new ToggleSwitch();

    // Settings components
    private final List<PremiumRenderableSetting> settings = new ArrayList<>();

    // Animation state
    private final AnimationManager hoverAnim;
    private final AnimationManager expandAnim;
    private final AnimationManager enableAnim;

    // Ripple
    private double rippleX, rippleY, rippleRadius;
    private double rippleAlpha = 0;
    private final AnimationManager rippleRadiusAnim;
    private final AnimationManager rippleAlphaAnim;

    private boolean expanded  = false;
    private boolean hovered   = false;

    // Position set externally each frame
    public int x, y, w;

    public ModuleCard(Module module) {
        this.module = module;
        hoverAnim        = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
        expandAnim       = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 10.0);
        enableAnim       = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO, 12.0);
        rippleRadiusAnim = new AnimationManager(0.0, AnimationManager.Easing.EASE_OUT_EXPO,  7.0);
        rippleAlphaAnim  = new AnimationManager(0.0, AnimationManager.Easing.LINEAR,          6.0);

        buildSettings();
    }

    private void buildSettings() {
        int off = 0;
        for (Setting<?> s : module.getSettings()) {
            PremiumRenderableSetting rs;
            if      (s instanceof BooleanSetting b)  rs = new PremiumCheckBox(b);
            else if (s instanceof NumberSetting  n)  rs = new PremiumSlider(n);
            else if (s instanceof ModeSetting<?> m)  rs = new PremiumModeBox(m);
            else if (s instanceof KeybindSetting k)  rs = new PremiumKeybindBox(k);
            else continue;
            rs.index = off++;
            settings.add(rs);
        }
    }

    /* ── Render ──────────────────────────────────────────────────── */

    /**
     * Render this card.  x/y/w must be set before calling.
     *
     * @return the total rendered height (card + expanded settings)
     */
    public int render(DrawContext context, int mouseX, int mouseY, float delta) {
        ThemeManager tm = ThemeManager.INSTANCE;

        hovered = isBaseHovered(mouseX, mouseY);
        hoverAnim.setTarget(hovered ? 1.0 : 0.0);
        expandAnim.setTarget(expanded ? 1.0 : 0.0);
        enableAnim.setTarget(module.isEnabled() ? 1.0 : 0.0);

        double hT = hoverAnim.update();
        double eT = expandAnim.update();
        double enT = enableAnim.update();

        int totalH = getTotalHeight();

        /* ── Background ──────────────────────────────────────────── */
        Color baseBg = tm.getCardBg();
        Color bg     = new Color(baseBg.getRed(), baseBg.getGreen(), baseBg.getBlue(), 200);
        RenderUtils.renderRoundedQuad(context, bg, x, y, x + w, y + CARD_H, RADIUS, 10);

        /* ── Hover glow ──────────────────────────────────────────── */
        if (hT > 0.01) {
            Color accent = tm.getAccent();
            Color glow   = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int)(25 * hT));
            RenderUtils.renderRoundedQuad(context, glow, x, y, x + w, y + CARD_H, RADIUS, 8);
        }

        /* ── Enable glow (left border) ───────────────────────────── */
        if (enT > 0.01) {
            Color accent = tm.getAccent();
            Color bar    = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int)(220 * enT));
            RenderUtils.renderRoundedQuad(context, bar, x, y + 8, x + 3, y + CARD_H - 8, 2, 6);
        }

        /* ── Ripple ──────────────────────────────────────────────── */
        rippleRadiusAnim.update();
        rippleAlphaAnim.update();
        double rR = rippleRadiusAnim.getValue();
        double rA = rippleAlphaAnim.getValue();
        if (rA > 0.01 && rR > 0) {
            Color rc = new Color(255, 255, 255, (int)(rA * 60));
            RenderUtils.renderCircle(context, rc, (int)rippleX, (int)rippleY, (int)rR, 20);
        }

        /* ── Border ──────────────────────────────────────────────── */
        Color borderBase = new Color(255, 255, 255, (int)(20 + 20 * hT));
        RenderUtils.renderRoundedOutline(context, borderBase, x, y, x + w, y + CARD_H,
                RADIUS, RADIUS, expanded ? 0 : RADIUS, expanded ? 0 : RADIUS, 1, 10);

        /* ── Module name ─────────────────────────────────────────── */
        Color nameColor = module.isEnabled() ? tm.getAccentBright() : tm.getTextPrimary();
        if (enT < 1.0 && enT > 0.0) {
            // Interpolate colour
            nameColor = interpolate(tm.getTextPrimary(), tm.getAccentBright(), enT);
        }
        TextRenderer.drawString(module.getName().toString(), context,
                x + 16, y + CARD_H / 2 - 6, nameColor.getRGB());

        /* ── Description ─────────────────────────────────────────── */
        if (module.getDescription() != null) {
            String desc = module.getDescription().toString();
            if (desc.length() > 30) desc = desc.substring(0, 27) + "...";
            TextRenderer.drawString(desc, context,
                    x + 16, y + CARD_H / 2 + 5, tm.getTextMuted().getRGB());
        }

        /* ── Toggle ──────────────────────────────────────────────── */
        int tX = x + w - ToggleSwitch.W - 14;
        int tY = y + (CARD_H - ToggleSwitch.H) / 2;
        boolean toggleHovered = toggle.isHovered(mouseX, mouseY, tX, tY);
        toggle.render(context, tX, tY, module.isEnabled(), toggleHovered);

        /* ── Settings expand ─────────────────────────────────────── */
        if (eT > 0.01 && !settings.isEmpty()) {
            renderSettings(context, mouseX, mouseY, eT);
        }

        return totalH;
    }

    private void renderSettings(DrawContext context, int mouseX, int mouseY, double eT) {
        ThemeManager tm = ThemeManager.INSTANCE;

        // Settings background
        int sY = y + CARD_H;
        int clipH = (int)(getSettingsHeight() * eT);

        // Border continuation
        Color sBorder = new Color(255, 255, 255, 18);
        RenderUtils.renderRoundedOutline(context, sBorder, x, sY, x + w, sY + getSettingsHeight(),
                0, 0, RADIUS, RADIUS, 1, 8);

        Color settingsBg = new Color(0, 0, 0, (int)(40 * eT));
        RenderUtils.renderRoundedQuad(context, settingsBg, x, sY, x + w, sY + getSettingsHeight(),
                0, 0, RADIUS, RADIUS, 8);

        int settingY = sY + SETTINGS_PAD;
        for (PremiumRenderableSetting rs : settings) {
            rs.render(context, x + 8, settingY, w - 16, SETTING_H, mouseX, mouseY, eT);
            settingY += SETTING_H;
        }
    }

    /* ── Input ───────────────────────────────────────────────────── */

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isBaseHovered((int)mouseX, (int)mouseY)) return;

        int tX = x + w - ToggleSwitch.W - 14;
        int tY = y + (CARD_H - ToggleSwitch.H) / 2;

        if (toggle.isClicked(mouseX, mouseY, button, tX, tY)) {
            module.toggle();
            spawnRipple(tX + ToggleSwitch.W / 2.0, tY + ToggleSwitch.H / 2.0);
            return;
        }

        if (button == 0) {
            // Left click on card body → expand / collapse settings
            if (!settings.isEmpty()) {
                expanded = !expanded;
                spawnRipple(mouseX, mouseY);
            }
        } else if (button == 1) {
            // Right click → toggle module
            module.toggle();
            spawnRipple(mouseX, mouseY);
        }

        // Delegate to settings
        if (expanded) {
            int sY = y + CARD_H + SETTINGS_PAD;
            for (PremiumRenderableSetting rs : settings) {
                rs.mouseClicked(mouseX, mouseY, button, x + 8, sY, w - 16, SETTING_H);
                sY += SETTING_H;
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dX, double dY) {
        if (!expanded) return;
        int sY = y + CARD_H + SETTINGS_PAD;
        for (PremiumRenderableSetting rs : settings) {
            rs.mouseDragged(mouseX, mouseY, button, dX, dY, x + 8, sY, w - 16, SETTING_H);
            sY += SETTING_H;
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (PremiumRenderableSetting rs : settings)
            rs.mouseReleased(mouseX, mouseY, button);
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (PremiumRenderableSetting rs : settings)
            rs.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && expanded)
            expanded = false;
    }

    /* ── Ripple ──────────────────────────────────────────────────── */

    private void spawnRipple(double rx, double ry) {
        rippleX = rx;
        rippleY = ry;
        rippleRadiusAnim.snap(0.0);
        rippleAlphaAnim.snap(1.0);
        rippleRadiusAnim.setTarget(Math.max(w, CARD_H) * 1.4);
        rippleAlphaAnim.setTarget(0.0);
    }

    /* ── Geometry ────────────────────────────────────────────────── */

    public int getTotalHeight() {
        if (settings.isEmpty()) return CARD_H + MARGIN;
        double eT = expandAnim.getValue();
        return (int)(CARD_H + getSettingsHeight() * eT + MARGIN);
    }

    public int getFullExpandedHeight() {
        return CARD_H + (settings.isEmpty() ? 0 : getSettingsHeight()) + MARGIN;
    }

    private int getSettingsHeight() {
        return settings.size() * SETTING_H + SETTINGS_PAD * 2;
    }

    private boolean isBaseHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + CARD_H;
    }

    /* ── Search matching ──────────────────────────────────────────── */

    public boolean matchesQuery(String query) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase();
        if (module.getName().toString().toLowerCase().contains(q)) return true;
        if (module.getDescription() != null &&
                module.getDescription().toString().toLowerCase().contains(q)) return true;
        for (PremiumRenderableSetting rs : settings) {
            if (rs.matchesQuery(q)) return true;
        }
        return false;
    }

    /* ── Helpers ─────────────────────────────────────────────────── */

    private static Color interpolate(Color a, Color b, double t) {
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    public Module getModule() { return module; }
}
