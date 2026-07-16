package dev.lvstrng.argon.gui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.*;
import java.nio.file.*;

/**
 * ThemeManager — accent color, built-in themes, dark/light toggle, import/export.
 *
 * All colours stored as ARGB ints.  Call ThemeManager.INSTANCE.getAccent() anywhere
 * you need the current accent tint.
 */
public final class ThemeManager {

    public static final ThemeManager INSTANCE = new ThemeManager();

    /* ── Built-in themes ──────────────────────────────────────────── */

    public enum BuiltinTheme {
        ARGON   ("Argon",   0xFF7C5CBF, 0xFF1A1A2E),
        AZURE   ("Azure",   0xFF4B9EFF, 0xFF0D1B2A),
        ROSE    ("Rose",    0xFFFF6B8A, 0xFF1E0A13),
        EMERALD ("Emerald", 0xFF3DD68C, 0xFF0A1F14),
        AMBER   ("Amber",   0xFFFFB347, 0xFF1E1408),
        VOID    ("Void",    0xFFAAAAAA, 0xFF111111);

        public final String displayName;
        public final int    accent;
        public final int    background;

        BuiltinTheme(String displayName, int accent, int background) {
            this.displayName = displayName;
            this.accent      = accent;
            this.background  = background;
        }
    }

    /* ── State ────────────────────────────────────────────────────── */

    private Color  accent        = new Color(0xFF7C5CBF);
    private Color  panelBg       = new Color(0x18, 0x18, 0x28, 220);
    private Color  sidebarBg     = new Color(0x12, 0x12, 0x1E, 230);
    private Color  cardBg        = new Color(0x1F, 0x1F, 0x33, 200);
    private Color  textPrimary   = new Color(0xF0F0F0);
    private Color  textSecondary = new Color(0xA0A0B8);
    private Color  textMuted     = new Color(0x60606A);
    private boolean darkMode     = true;
    private BuiltinTheme activeBuiltin = BuiltinTheme.ARGON;

    private static final Path THEME_FILE = MinecraftClient.getInstance()
            .runDirectory.toPath().resolve("argon_theme.json");

    private ThemeManager() {
        load();
    }

    /* ── Getters ──────────────────────────────────────────────────── */

    public Color getAccent()        { return accent; }
    public Color getPanelBg()       { return panelBg; }
    public Color getSidebarBg()     { return sidebarBg; }
    public Color getCardBg()        { return cardBg; }
    public Color getTextPrimary()   { return textPrimary; }
    public Color getTextSecondary() { return textSecondary; }
    public Color getTextMuted()     { return textMuted; }
    public boolean isDarkMode()     { return darkMode; }
    public BuiltinTheme getActiveBuiltin() { return activeBuiltin; }

    /** Accent at reduced opacity (for glows, card highlights). */
    public Color getAccentSoft(int alpha) {
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha);
    }

    /** Lighter version of accent for text on dark surfaces. */
    public Color getAccentBright() {
        float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        return Color.getHSBColor(hsb[0], Math.max(0.3f, hsb[1] - 0.2f), Math.min(1.0f, hsb[2] + 0.25f));
    }

    /* ── Setters ──────────────────────────────────────────────────── */

    public void setAccent(Color c) {
        this.accent = c;
        save();
    }

    public void setAccentRGB(int rgb) {
        setAccent(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
    }

    public void applyBuiltin(BuiltinTheme theme) {
        this.activeBuiltin = theme;
        this.accent  = new Color(theme.accent, true);
        Color bg     = new Color(theme.background, true);
        this.panelBg   = withAlpha(bg, 220);
        this.sidebarBg = withAlpha(bg.darker(), 230);
        this.cardBg    = withAlpha(bg.brighter(), 200);
        save();
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        if (dark) {
            textPrimary   = new Color(0xF0F0F0);
            textSecondary = new Color(0xA0A0B8);
            textMuted     = new Color(0x60606A);
        } else {
            textPrimary   = new Color(0x18181E);
            textSecondary = new Color(0x50506A);
            textMuted     = new Color(0xA0A0B8);
            panelBg   = new Color(0xF2F2F8, false);
            sidebarBg = new Color(0xE8E8F0, false);
            cardBg    = new Color(0xFFFFFF, false);
        }
        save();
    }

    /* ── Persistence ──────────────────────────────────────────────── */

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("accent",   accent.getRGB());
            obj.addProperty("darkMode", darkMode);
            obj.addProperty("builtin",  activeBuiltin.name());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(THEME_FILE, gson.toJson(obj));
        } catch (IOException ignored) {}
    }

    public void load() {
        if (!Files.exists(THEME_FILE)) return;
        try {
            String json = Files.readString(THEME_FILE);
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            if (obj.has("builtin")) {
                try { applyBuiltin(BuiltinTheme.valueOf(obj.get("builtin").getAsString())); }
                catch (IllegalArgumentException ignored) {}
            }
            if (obj.has("accent")) {
                setAccentRGB(obj.get("accent").getAsInt());
            }
            if (obj.has("darkMode")) {
                setDarkMode(obj.get("darkMode").getAsBoolean());
            }
        } catch (IOException | com.google.gson.JsonParseException ignored) {}
    }

    /** Export theme JSON string (for copy-paste sharing). */
    public String exportJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("accent",   accent.getRGB());
        obj.addProperty("darkMode", darkMode);
        obj.addProperty("builtin",  activeBuiltin.name());
        return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
    }

    /** Import from JSON string. Returns true on success. */
    public boolean importJson(String json) {
        try {
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            if (obj.has("builtin"))
                applyBuiltin(BuiltinTheme.valueOf(obj.get("builtin").getAsString()));
            if (obj.has("accent"))
                setAccentRGB(obj.get("accent").getAsInt());
            if (obj.has("darkMode"))
                setDarkMode(obj.get("darkMode").getAsBoolean());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ── Helpers ──────────────────────────────────────────────────── */

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
