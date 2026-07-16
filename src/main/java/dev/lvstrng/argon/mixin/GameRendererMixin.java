package dev.lvstrng.argon.mixin;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.EventManager;
import dev.lvstrng.argon.event.events.GameRenderListener;
import dev.lvstrng.argon.module.modules.misc.Freecam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

	@Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
	private void onWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
		MatrixStack matrixStack = new MatrixStack();
		EventManager.fire(new GameRenderListener.GameRenderEvent(matrixStack, tickCounter.getTickProgress(true)));
	}

	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
		if (Argon.INSTANCE.getModuleManager().getModule(Freecam.class).isEnabled())
			cir.setReturnValue(false);
	}
}
