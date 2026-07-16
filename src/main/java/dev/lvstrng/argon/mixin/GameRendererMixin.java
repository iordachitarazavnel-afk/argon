package dev.lvstrng.argon.mixin;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.EventManager;
import dev.lvstrng.argon.event.events.GameRenderListener;
import dev.lvstrng.argon.module.modules.misc.Freecam;
import dev.lvstrng.argon.utils.RotationManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow public abstract Matrix4f getBasicProjectionMatrix(float fov);
    @Shadow protected abstract float getFov(Camera camera, float tickProgress, boolean changingFov);
    @Shadow @Final private Camera camera;

    // ===== REDIRECT CAMERA ROTATION FOR SILENT ROTATIONS =====
    @Redirect(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;getRotation()Lorg/joml/Quaternionf;"
        )
    )
    private Quaternionf redirectCameraRotation(Camera camera) {
        // Check if silent rotation is active and not in third person
        if (RotationManager.isSilentRotationActive() && !camera.isThirdPerson()) {
            float visualYaw = RotationManager.getVisualYaw() + 180.0F;
            float visualPitch = RotationManager.getVisualPitch();

            if (Float.isFinite(visualYaw) && Float.isFinite(visualPitch)) {
                Quaternionf customRotation = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
                customRotation.mul(new Quaternionf().rotationYXZ(
                    (float) Math.toRadians(-visualYaw),
                    (float) Math.toRadians(visualPitch),
                    0.0F
                ));

                if (Float.isFinite(customRotation.x) && Float.isFinite(customRotation.y)
                    && Float.isFinite(customRotation.z) && Float.isFinite(customRotation.w)) {
                    return customRotation;
                }
            }
        }
        return camera.getRotation();
    }

    // ===== EXISTING CODE: WORLD RENDER EVENT =====
    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 1))
    private void onWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
        float tickDelta = 1.0F;
        MatrixStack matrixStack = new MatrixStack();

        if (camera != null) {
            Vec3d cameraPos = camera.getFocusedEntity() != null
                    ? new Vec3d(camera.getFocusedEntity().getX(), camera.getFocusedEntity().getY(), camera.getFocusedEntity().getZ())
                    : Vec3d.ZERO;
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        }

        EventManager.fire(new GameRenderListener.GameRenderEvent(matrixStack, tickDelta));
    }

    // ===== BLOCK OUTLINE =====
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        // Poți adăuga aici condiții pentru a ascunde outline-ul blocului
        // dacă SafeAnchor este activ
    }
}
