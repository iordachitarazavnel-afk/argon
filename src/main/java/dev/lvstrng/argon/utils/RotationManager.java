package dlindustries.vigillant.system.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class RotationManager {
    private static final Minecraft mc = Minecraft.getInstance();
    
    private static boolean isActive = false;
    private static boolean isSilent = false;
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    private static float visualYaw = 0;
    private static float visualPitch = 0;
    private static float finalYaw = 0;
    private static float finalPitch = 0;
    private static float currentYaw = 0;
    private static float currentPitch = 0;
    private static boolean isSmooth = true;
    private static float smoothSpeed = 0.2f;
    private static long lastUpdateTime = 0;
    private static Runnable onRotationComplete = null;
    private static boolean rotationComplete = false;
    
    // Perspectivă și moduri
    public static boolean perspective = false;
    public static MovementMode movementMode = MovementMode.CLIENT;
    public static RotationMode rotationMode = RotationMode.CLIENT;
    
    public enum MovementMode {
        CLIENT, SERVER, BOTH
    }
    
    public enum RotationMode {
        CLIENT, SERVER, BOTH
    }

    public static void setTargetRotation(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = MathHelper.clamp(pitch, -90f, 90f);
        isActive = true;
        isSilent = true;
        rotationComplete = false;
        
        if (mc.player != null) {
            currentYaw = mc.player.getYRot();
            currentPitch = mc.player.getXRot();
            finalYaw = currentYaw;
            finalPitch = currentPitch;
            visualYaw = currentYaw;
            visualPitch = currentPitch;
        }
        
        lastUpdateTime = System.currentTimeMillis();
    }

    public static void setTargetRotationInstant(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = MathHelper.clamp(pitch, -90f, 90f);
        isActive = true;
        isSilent = true;
        isSmooth = false;
        rotationComplete = false;
        
        if (mc.player != null) {
            currentYaw = mc.player.getYRot();
            currentPitch = mc.player.getXRot();
            finalYaw = yaw;
            finalPitch = targetPitch;
            visualYaw = yaw;
            visualPitch = targetPitch;
        }
        
        lastUpdateTime = System.currentTimeMillis();
    }

    public static void update(float tickDelta) {
        if (!isActive || mc.player == null) return;
        
        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastUpdateTime) / 16.667f, 3.0f);
        lastUpdateTime = now;
        
        if (isSmooth) {
            // Smooth interpolation
            float yawDiff = MathHelper.wrapDegrees(targetYaw - finalYaw);
            float pitchDiff = targetPitch - finalPitch;
            
            if (Math.abs(yawDiff) > 0.1f || Math.abs(pitchDiff) > 0.1f) {
                float speed = smoothSpeed * delta;
                finalYaw += yawDiff * speed;
                finalPitch += pitchDiff * speed;
                
                finalPitch = MathHelper.clamp(finalPitch, -90f, 90f);
            } else {
                finalYaw = targetYaw;
                finalPitch = targetPitch;
                rotationComplete = true;
                if (onRotationComplete != null) {
                    onRotationComplete.run();
                    onRotationComplete = null;
                }
            }
        } else {
            finalYaw = targetYaw;
            finalPitch = targetPitch;
            rotationComplete = true;
            if (onRotationComplete != null) {
                onRotationComplete.run();
                onRotationComplete = null;
            }
        }
        
        // Actualizează rotațiile vizuale
        visualYaw = finalYaw;
        visualPitch = finalPitch;
        
        // Aplică rotațiile pe client
        if (movementMode == MovementMode.CLIENT || movementMode == MovementMode.BOTH) {
            mc.player.setYRot(finalYaw);
            mc.player.setXRot(finalPitch);
        }
    }

    public static void setOnRotationComplete(Runnable callback) {
        onRotationComplete = callback;
    }

    public static boolean isRotationComplete() {
        return rotationComplete;
    }

    public static void clearRotation() {
        isActive = false;
        isSilent = false;
        rotationComplete = false;
        onRotationComplete = null;
    }

    public static boolean isActive() {
        return isActive;
    }

    public static boolean isSilentRotationActive() {
        return isActive && isSilent;
    }

    public static float getVisualYaw() {
        return isActive ? visualYaw : (mc.player != null ? mc.player.getYRot() : 0);
    }

    public static float getVisualPitch() {
        return isActive ? visualPitch : (mc.player != null ? mc.player.getXRot() : 0);
    }

    public static float getFinalYaw() {
        return finalYaw;
    }

    public static float getFinalPitch() {
        return finalPitch;
    }

    public static void setSmoothSpeed(float speed) {
        smoothSpeed = MathHelper.clamp(speed, 0.01f, 1.0f);
    }

    // Calculul rotațiilor către o poziție
    public static float[] calculateRotationsTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        
        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distance));
        
        return new float[]{yaw, pitch};
    }

    public static float[] calculateRotationsToBlock(Vec3d blockPos) {
        if (mc.player == null) return new float[]{0, 0};
        
        Vec3d eyePos = mc.player.getEyePosition();
        Vec3d targetPos = blockPos.add(0.5, 0.5, 0.5);
        
        return calculateRotationsTo(eyePos, targetPos);
    }

    // Face un flick de rotație (pentru SafeAnchor)
    public static void flickTick() {
        if (mc.player == null) return;
        
        // Aplică rotația instantaneu
        mc.player.setYRot(finalYaw);
        mc.player.setXRot(finalPitch);
        
        // Resetează după un tick
        isActive = false;
        rotationComplete = true;
    }
}
