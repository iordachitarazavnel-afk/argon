package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.event.events.TickEvent;
import dev.lvstrng.argon.event.listeners.TickListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.RotationManager;
import dev.lvstrng.argon.utils.RotationTickHandler;
import net.minecraft.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SafeAnchor extends Module implements TickListener {
    
    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 0.3, 0.05, 1.0);
    private final BooleanSetting silentRotations = new BooleanSetting("Silent Rotations", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    private final BooleanSetting visualFeedback = new BooleanSetting("Visual Feedback", true);
    
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
    private boolean rotationComplete = false;
    
    public SafeAnchor() {
        super("Safe Anchor", "Places a safe respawn anchor with silent rotations", Category.COMBAT);
        addSettings(range, rotationSpeed, silentRotations, autoSwitch, visualFeedback);
        RotationTickHandler.register();
    }
    
    @Override
    public void onEnable() {
        currentState = State.IDLE;
        anchorPos = null;
        glowstonePos = null;
        tickCounter = 0;
        rotationComplete = false;
        eventManager.add(TickEvent.class, this);
        super.onEnable();
    }
    
    @Override
    public void onDisable() {
        eventManager.remove(TickEvent.class, this);
        RotationManager.clearRotation();
        currentState = State.IDLE;
        super.onDisable();
    }
    
    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.level == null) return;
        
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
        if (mc.player == null || mc.level == null) return;
        
        BlockPos targetPos = findValidAnchorPosition();
        if (targetPos == null) return;
        
        anchorPos = targetPos;
        glowstonePos = anchorPos.above();
        
        if (!mc.level.getBlockState(glowstonePos).isAir()) return;
        
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = Vec3.atCenterOf(anchorPos);
        float[] rotations = RotationManager.calculateRotationsTo(eyePos, targetCenter);
        
        if (silentRotations.getValue()) {
            RotationManager.setSmoothSpeed((float) rotationSpeed.getValue());
            RotationManager.setOnRotationComplete(() -> {
                rotationComplete = true;
            });
            RotationManager.setTargetRotation(rotations[0], rotations[1]);
        } else {
            mc.player.setYRot(rotations[0]);
            mc.player.setXRot(rotations[1]);
            rotationComplete = true;
        }
        
        currentState = State.ROTATING_TO_ANCHOR;
        tickCounter = 0;
        rotationComplete = false;
    }
    
    private void handleRotatingToAnchor() {
        if (rotationComplete || !silentRotations.getValue()) {
            currentState = State.PLACING_ANCHOR;
            rotationComplete = false;
        }
        
        if (tickCounter > 40) {
            currentState = State.PLACING_ANCHOR;
        }
    }
    
    private void handlePlacingAnchor() {
        if (mc.player == null || mc.level == null) return;
        
        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.RESPAWN_ANCHOR);
            if (slot == -1) {
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().selected = slot;
        }
        
        boolean placed = placeBlock(anchorPos);
        
        if (placed) {
            if (visualFeedback.getValue()) {
                mc.particleEngine.createBlockParticles(anchorPos, mc.level.getBlockState(anchorPos), 3);
            }
            
            currentState = State.ROTATING_TO_GLOWSTONE;
            tickCounter = 0;
            rotationComplete = false;
            
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 targetCenter = Vec3.atCenterOf(glowstonePos);
            float[] rotations = RotationManager.calculateRotationsTo(eyePos, targetCenter);
            
            if (silentRotations.getValue()) {
                RotationManager.setSmoothSpeed((float) rotationSpeed.getValue());
                RotationManager.setOnRotationComplete(() -> {
                    rotationComplete = true;
                });
                RotationManager.setTargetRotation(rotations[0], rotations[1]);
            } else {
                mc.player.setYRot(rotations[0]);
                mc.player.setXRot(rotations[1]);
                rotationComplete = true;
            }
        } else {
            currentState = State.IDLE;
        }
    }
    
    private void handleRotatingToGlowstone() {
        if (rotationComplete || !silentRotations.getValue()) {
            currentState = State.PLACING_GLOWSTONE;
            rotationComplete = false;
        }
        
        if (tickCounter > 40) {
            currentState = State.PLACING_GLOWSTONE;
        }
    }
    
    private void handlePlacingGlowstone() {
        if (mc.player == null || mc.level == null) return;
        
        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.GLOWSTONE);
            if (slot == -1) {
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().selected = slot;
        }
        
        boolean placed = placeBlock(glowstonePos);
        
        if (placed) {
            if (visualFeedback.getValue()) {
                mc.particleEngine.createBlockParticles(glowstonePos, mc.level.getBlockState(glowstonePos), 3);
            }
            currentState = State.COMPLETE;
            RotationManager.clearRotation();
            
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a✅ Safe anchor placed at " + 
                    anchorPos.getX() + ", " + anchorPos.getY() + ", " + anchorPos.getZ()),
                    true
                );
            }
        } else {
            currentState = State.IDLE;
        }
    }
    
    private boolean placeBlock(BlockPos pos) {
        if (mc.player == null || mc.level == null) return false;
        if (mc.gameMode == null) return false;
        
        if (!mc.level.getBlockState(pos).isAir()) return false;
        
        Direction dir = getPlaceDirection(pos);
        if (dir == null) return false;
        
        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atCenterOf(pos),
            dir,
            pos,
            false
        );
        
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        return !mc.level.getBlockState(pos).isAir();
    }
    
    private Direction getPlaceDirection(BlockPos pos) {
        if (mc.player == null) return null;
        
        Vec3 playerPos = mc.player.position();
        Vec3 blockPos = Vec3.atCenterOf(pos);
        Vec3 diff = blockPos.subtract(playerPos);
        
        if (Math.abs(diff.x) > Math.abs(diff.z)) {
            return diff.x > 0 ? Direction.WEST : Direction.EAST;
        } else {
            return diff.z > 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }
    
    private BlockPos findValidAnchorPosition() {
        if (mc.player == null || mc.level == null) return null;
        
        int radius = (int) Math.floor(range.getValue());
        BlockPos playerPos = mc.player.blockPosition();
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    double dist = playerPos.distSqr(pos);
                    if (dist > range.getValue() * range.getValue()) continue;
                    if (dist < 1.0) continue;
                    
                    if (isValidAnchorPosition(pos)) {
                        return pos;
                    }
                }
            }
        }
        
        return null;
    }
    
    private boolean isValidAnchorPosition(BlockPos pos) {
        if (mc.level == null) return false;
        
        BlockPos below = pos.below();
        if (!mc.level.getBlockState(below).isSolid()) return false;
        
        if (!mc.level.getBlockState(pos).isAir()) return false;
        
        BlockPos above = pos.above();
        if (!mc.level.getBlockState(above).isAir()) return false;
        
        return true;
    }
    
    private int findItemSlot(net.minecraft.world.item.Item item) {
        if (mc.player == null) return -1;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
