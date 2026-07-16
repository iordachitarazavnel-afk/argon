package dev.lvstrng.argon.gui.notification;

import dev.lvstrng.argon.gui.animation.AnimationManager;
import dev.lvstrng.argon.gui.theme.ThemeManager;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * NotificationManager — floating toast notifications.
 *
 * Usage anywhere: NotificationManager.INSTANCE.push("Title", "Message", Type.INFO);
 * Call NotificationManager.INSTANCE.render(context, delta) each frame (from ClickGui or HUD).
 */
public final class NotificationManager {

    public static final NotificationManager INSTANCE = new NotificationManager();

    /* ── Types ───────────────────────────────────────────────────── */

    public enum Type {
        INFO, SUCCESS, WARNING, ERROR;

        public Color getColor() {
            return switch (this) {
                case INFO    -> ThemeManager.INSTANCE.getAccent();
                case SUCCESS -> new Color(0x3D, 0xD6, 0x8C);
                case WARNING -> new Color(0xFF, 0xB3, 0x47);
                case ERROR   -> new Color(0xFF, 0x5C, 0x5C);
            };
        }
    }

    /* ── Notification entry ──────────────────────────────────────── */

    private static final class Notification {
        final String  title;
        final String  message;
        final Type    type;
        /** ms until auto-dismiss */
        long duration;
        long spawnTime = System.currentTimeMillis();

        final AnimationManager slideAnim  = new AnimationManager(0, AnimationManager.Easing.EASE_OUT_EXPO, 14);
        final AnimationManager alphaAnim  = AnimationManager.forFade();
        final AnimationManager progressAnim;

        boolean dismissed = false;

        Notification(String title, String message, Type type, long durationMs) {
            this.title    = title;
            this.message  = message;
            this.type     = type;
            this.duration = durationMs;
            this.progressAnim = new AnimationManager(1.0, AnimationManager.Easing.LINEAR, 1.0 / (durationMs / 1000.0));
            slideAnim.setTarget(1.0);
            alphaAnim.setTarget(1.0);
        }

        void dismiss() {
            if (!dismissed) {
                dismissed = true;
                slideAnim.setTarget(0.0);
                alphaAnim.setTarget(0.0);
            }
        }

        boolean canRemove() {
            return dismissed && alphaAnim.isFinished();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - spawnTime >= duration;
        }
    }

    /* ── State ───────────────────────────────────────────────────── */

    private final List<Notification> notifications = new ArrayList<>();

    private static final int NOTIF_WIDTH   = 280;
    private static final int NOTIF_HEIGHT  = 58;
    private static final int NOTIF_PADDING = 10;
    private static final int NOTIF_RADIUS  = 10;
    private static final int BAR_HEIGHT    = 3;

    private NotificationManager() {}

    /* ── Public API ──────────────────────────────────────────────── */

    public void push(String title, String message, Type type) {
        push(title, message, type, 4000L);
    }

    public void push(String title, String message, Type type, long durationMs) {
        synchronized (notifications) {
            notifications.add(new Notification(title, message, type, durationMs));
        }
    }

    /* ── Render ──────────────────────────────────────────────────── */

    public void render(DrawContext context, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getWidth();
        int screenH = mc.getWindow().getHeight();

        synchronized (notifications) {
            Iterator<Notification> it = notifications.iterator();
            int stackY = screenH - NOTIF_PADDING;

            // Iterate bottom-to-top so newer ones stack higher
            List<Notification> copy = new ArrayList<>(notifications);
            for (int i = copy.size() - 1; i >= 0; i--) {
                Notification n = copy.get(i);

                if (n.isExpired()) n.dismiss();

                double slide = n.slideAnim.update();
                double alpha = n.alphaAnim.update();
                n.progressAnim.update();

                if (alpha <= 0.01) continue;

                int a   = (int) (alpha * 255);
                int x   = (int) (screenW - NOTIF_PADDING - NOTIF_WIDTH * slide);
                int y   = stackY - NOTIF_HEIGHT;

                ThemeManager tm = ThemeManager.INSTANCE;
                Color bg        = new Color(tm.getPanelBg().getRed(),
                                            tm.getPanelBg().getGreen(),
                                            tm.getPanelBg().getBlue(), a);
                Color accent    = n.type.getColor();
                Color accentA   = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a);

                // Panel background
                RenderUtils.renderRoundedQuad(context, bg,
                        x, y, x + NOTIF_WIDTH, y + NOTIF_HEIGHT, NOTIF_RADIUS, 10);

                // Left accent bar
                RenderUtils.renderRoundedQuad(context,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a),
                        x, y, x + 3, y + NOTIF_HEIGHT, NOTIF_RADIUS, 0, NOTIF_RADIUS, 0, 10);

                // Progress bar at bottom
                double progress = n.progressAnim.getValue();
                int barW = (int) ((NOTIF_WIDTH) * progress);
                if (barW > 0) {
                    RenderUtils.renderRoundedQuad(context, accentA,
                            x, y + NOTIF_HEIGHT - BAR_HEIGHT, x + barW, y + NOTIF_HEIGHT, 0, 0, NOTIF_RADIUS, NOTIF_RADIUS, 5);
                }

                // Text
                Color titleC = new Color(tm.getTextPrimary().getRed(),
                                         tm.getTextPrimary().getGreen(),
                                         tm.getTextPrimary().getBlue(), a);
                Color msgC   = new Color(tm.getTextSecondary().getRed(),
                                         tm.getTextSecondary().getGreen(),
                                         tm.getTextSecondary().getBlue(), a);
                TextRenderer.drawString(n.title,   context, x + 14, y + 12, titleC.getRGB());
                TextRenderer.drawString(n.message, context, x + 14, y + 30, msgC.getRGB());

                stackY -= NOTIF_HEIGHT + NOTIF_PADDING;

                if (n.canRemove()) notifications.remove(n);
            }
        }
    }
}
