package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.rotation.RotatorManager;
import dev.lvstrng.argon.utils.rotation.Rotation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SafeAnchor extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.1);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 0.3, 0.05, 1.0, 0.05);
    private final BooleanSetting silentRotations = new BooleanSetting("Silent Rotations", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    private enum State {
        IDLE,
        ROTATING_TO_ANCHOR,
        PLACING_ANCHOR,
        ROTATING_TO_GLOWSTONE,
        PLACING_GLOWSTONE,
        COMPLETE
    }

    private State currentState = State.IDLE;
    private BlockPos anchorPos = null;
    private BlockPos glowstonePos = null;
    private int tickCounter = 0;
    private boolean rotationDone = false;

    public SafeAnchor() {
        super("Safe Anchor", "Places a safe respawn anchor silently", 0, Category.COMBAT);
        addSettings(range, rotationSpeed, silentRotations, autoSwitch);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) {
                this.onTick();
            }
        });
    }

    @Override
    public void onEnable() {
        currentState = State.IDLE;
        anchorPos = null;
        glowstonePos = null;
        tickCounter = 0;
        rotationDone = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        RotatorManager rotator = Argon.INSTANCE.rotatorManager;
        if (rotator != null) {
            rotator.disable();
            // Revino la rotația normală
            if (mc.player != null) {
                rotator.setClientRotation(new Rotation(mc.player.getYaw(), mc.player.getPitch()));
            }
        }
        currentState = State.IDLE;
        super.onDisable();
    }

    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;

        tickCounter++;

        switch (currentState) {
            case IDLE:
                startPlacement();
                break;
            case ROTATING_TO_ANCHOR:
                handleRotatingToAnchor();
                break;
            case PLACING_ANCHOR:
                handlePlacingAnchor();
                break;
            case ROTATING_TO_GLOWSTONE:
                handleRotatingToGlowstone();
                break;
            case PLACING_GLOWSTONE:
                handlePlacingGlowstone();
                break;
            case COMPLETE:
                if (tickCounter % 20 == 0) {
                    currentState = State.IDLE;
                }
                break;
        }
    }

    private void startPlacement() {
        if (mc.player == null || mc.world == null) return;

        BlockPos targetPos = findValidAnchorPosition();
        if (targetPos == null) return;

        anchorPos = targetPos;
        glowstonePos = anchorPos.up();

        if (!mc.world.getBlockState(glowstonePos).isAir()) return;

        if (!hasItem(Items.RESPAWN_ANCHOR) || !hasItem(Items.GLOWSTONE)) {
            currentState = State.IDLE;
            return;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetCenter = Vec3d.ofCenter(anchorPos);
        float[] rotations = calculateRotationsTo(eyePos, targetCenter);

        RotatorManager rotator = Argon.INSTANCE.rotatorManager;

        if (silentRotations.getValue() && rotator != null) {
            // 🔥 SERVERUL CREEDE CĂ TE UIȚI LA BLOC
            rotator.setServerRotation(new Rotation(rotations[0], rotations[1]));
            // 🔥 TU VEI VEDEA ROTAȚIA NORMALĂ (sau ce vrei)
            rotator.setClientRotation(new Rotation(mc.player.getYaw(), mc.player.getPitch()));
            rotator.enable();
            rotationDone = true;
        } else {
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(rotations[1]);
            rotationDone = true;
        }

        currentState = State.PLACING_ANCHOR;
        tickCounter = 0;
    }

    private void handleRotatingToAnchor() {
        currentState = State.PLACING_ANCHOR;
    }

    private void handlePlacingAnchor() {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;

        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.RESPAWN_ANCHOR);
            if (slot == -1) {
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().setSelectedSlot(slot);
        }

        boolean placed = placeBlock(anchorPos);

        if (placed) {
            currentState = State.ROTATING_TO_GLOWSTONE;
            tickCounter = 0;
            rotationDone = false;

            Vec3d eyePos = mc.player.getEyePos();
            Vec3d targetCenter = Vec3d.ofCenter(glowstonePos);
            float[] rotations = calculateRotationsTo(eyePos, targetCenter);

            RotatorManager rotator = Argon.INSTANCE.rotatorManager;

            if (silentRotations.getValue() && rotator != null) {
                rotator.setServerRotation(new Rotation(rotations[0], rotations[1]));
                rotator.setClientRotation(new Rotation(mc.player.getYaw(), mc.player.getPitch()));
                rotator.enable();
                rotationDone = true;
            } else {
                mc.player.setYaw(rotations[0]);
                mc.player.setPitch(rotations[1]);
                rotationDone = true;
            }
        } else {
            currentState = State.IDLE;
        }
    }

    private void handleRotatingToGlowstone() {
        currentState = State.PLACING_GLOWSTONE;
    }

    private void handlePlacingGlowstone() {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;

        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.GLOWSTONE);
            if (slot == -1) {
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().setSelectedSlot(slot);
        }

        boolean placed = placeBlock(glowstonePos);

        if (placed) {
            currentState = State.COMPLETE;
            // 🔥 DEZACTIVEAZĂ ROTAȚIA SILENTĂ
            RotatorManager rotator = Argon.INSTANCE.rotatorManager;
            if (rotator != null) {
                rotator.disable();
                // Revino la rotația normală
                rotator.setClientRotation(new Rotation(mc.player.getYaw(), mc.player.getPitch()));
            }
        } else {
            currentState = State.IDLE;
        }
    }

    private boolean placeBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null) return false;
        if (mc.interactionManager == null) return false;

        if (!mc.world.getBlockState(pos).isAir()) return false;

        Direction dir = getPlaceDirection(pos);
        if (dir == null) return false;

        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(pos),
            dir,
            pos,
            false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        return !mc.world.getBlockState(pos).isAir();
    }

    private Direction getPlaceDirection(BlockPos pos) {
        if (mc.player == null) return null;

        BlockPos playerBlockPos = mc.player.getBlockPos();
        Vec3d blockPos = Vec3d.ofCenter(pos);
        Vec3d playerCenter = Vec3d.ofCenter(playerBlockPos);
        Vec3d diff = blockPos.subtract(playerCenter);

        if (Math.abs(diff.x) > Math.abs(diff.z)) {
            return diff.x > 0 ? Direction.WEST : Direction.EAST;
        } else {
            return diff.z > 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }

    private BlockPos findValidAnchorPosition() {
        if (mc.player == null || mc.world == null) return null;

        int radius = (int) Math.floor(range.getValue());
        BlockPos playerPos = mc.player.getBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);

                    double distSq = playerPos.getSquaredDistance(pos);
                    if (distSq > range.getValue() * range.getValue()) continue;
                    if (distSq < 1.0) continue;

                    if (isValidAnchorPosition(pos)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidAnchorPosition(BlockPos pos) {
        if (mc.world == null) return false;

        BlockPos below = pos.down();
        if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return false;
        }

        if (!mc.world.getBlockState(pos).isAir()) return false;

        BlockPos above = pos.up();
        if (!mc.world.getBlockState(above).isAir()) return false;

        return true;
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        if (mc.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasItem(net.minecraft.item.Item item) {
        return findItemSlot(item) != -1;
    }

    private float[] calculateRotationsTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distance));
        return new float[]{yaw, pitch};
    }

    @Override
    public String getHudInfo() {
        if (currentState == State.COMPLETE) return "§aDone";
        if (currentState != State.IDLE) return "§ePlacing...";
        return null;
    }

    @Override
    public int getHudInfoColor() {
        if (currentState == State.COMPLETE) return 0xFF00FF00;
        if (currentState != State.IDLE) return 0xFFFFFF00;
        return 0xFFFFFFFF;
    }
}
