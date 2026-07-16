package dev.lvstrng.argon.gui.animation;

import dev.lvstrng.argon.utils.RenderUtils;

/**
 * AnimationManager — frame-delta-independent easing for all GUI values.
 *
 * Usage:
 *   AnimationManager anim = new AnimationManager(0.0, Easing.EASE_OUT_EXPO);
 *   anim.setTarget(1.0);
 *   // each frame:
 *   double v = anim.update();
 */
public final class AnimationManager {

    public enum Easing {
        LINEAR,
        EASE_OUT_CUBIC,
        EASE_OUT_EXPO,
        EASE_OUT_BACK,
        EASE_IN_OUT_CUBIC
    }

    private double value;
    private double target;
    private final Easing easing;
    /** Multiplier: higher = faster. Default 10 ≈ ~60fps feel. */
    private double speed;

    public AnimationManager(double initial, Easing easing) {
        this.value  = initial;
        this.target = initial;
        this.easing = easing;
        this.speed  = 10.0;
    }

    public AnimationManager(double initial, Easing easing, double speed) {
        this.value  = initial;
        this.target = initial;
        this.easing = easing;
        this.speed  = speed;
    }

    /** Set desired end value. */
    public void setTarget(double target) {
        this.target = target;
    }

    /** Advance animation by one frame and return current value. */
    public double update() {
        double dt = RenderUtils.deltaTime();
        double t  = Math.min(1.0, dt * speed);
        value = value + (target - value) * ease(t);
        if (Math.abs(value - target) < 0.001) value = target;
        return value;
    }

    /** Advance and return value (same as update()). */
    public double getValue() {
        return value;
    }

    /** Returns true when animation has settled at target. */
    public boolean isFinished() {
        return value == target;
    }

    public void snap() {
        value = target;
    }

    public void snap(double v) {
        value  = v;
        target = v;
    }

    private double ease(double t) {
        return switch (easing) {
            case LINEAR             -> t;
            case EASE_OUT_CUBIC     -> 1.0 - Math.pow(1.0 - t, 3.0);
            case EASE_OUT_EXPO      -> t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
            case EASE_OUT_BACK      -> {
                double c1 = 1.70158, c3 = c1 + 1.0;
                yield 1.0 + c3 * Math.pow(t - 1.0, 3.0) + c1 * Math.pow(t - 1.0, 2.0);
            }
            case EASE_IN_OUT_CUBIC  -> t < 0.5
                    ? 4.0 * t * t * t
                    : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
        };
    }

    /** Convenience: create a 0→0 manager ready for toggle use. */
    public static AnimationManager forToggle() {
        return new AnimationManager(0.0, Easing.EASE_OUT_EXPO, 12.0);
    }

    /** Convenience: create a manager for alpha fades. */
    public static AnimationManager forFade() {
        return new AnimationManager(0.0, Easing.EASE_OUT_CUBIC, 8.0);
    }

    /** Convenience: create a manager for slide transitions. */
    public static AnimationManager forSlide() {
        return new AnimationManager(0.0, Easing.EASE_OUT_EXPO, 14.0);
    }
}
