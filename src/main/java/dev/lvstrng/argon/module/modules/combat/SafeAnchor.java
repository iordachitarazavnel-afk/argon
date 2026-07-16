package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.RotationManager;
import dev.lvstrng.argon.utils.RotationTickHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SafeAnchor extends Module {

    // ===== SETTINGS =====
    // NumberSetting: name, value, min, max, increment
    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.1);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 0.3, 0.05, 1.0, 0.05);
    private final BooleanSetting silentRotations = new BooleanSetting("Silent Rotations", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    private final BooleanSetting visualFeedback = new BooleanSetting("Visual Feedback", true);

    // ===== STATE MACHINE =====
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

    // ===== CONSTRUCTOR =====
    public SafeAnchor() {
        super("Safe Anchor", "Places a safe respawn anchor with silent rotations", 0, Category.COMBAT);
        addSettings(range, rotationSpeed, silentRotations, autoSwitch, visualFeedback);
        RotationTickHandler.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) {
                this.onTick();
            }
        });
    }

    // ===== MODULE LIFECYCLE =====
    @Override
    public void onEnable() {
        currentState = State.IDLE;
        anchorPos = null;
        glowstonePos = null;
        tickCounter = 0;
        rotationComplete = false;
        super.onEnable();
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§aSafe Anchor§7] §fEnabled"), false);
        }
    }

    @Override
    public void onDisable() {
        RotationManager.clearRotation();
        currentState = State.IDLE;
        super.onDisable();
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§aSafe Anchor§7] §fDisabled"), false);
        }
    }

    // ===== MAIN TICK LOGIC =====
    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;  // Yarn: interactionManager

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

    // ===== PLACEMENT LOGIC =====
    private void startPlacement() {
        if (mc.player == null || mc.world == null) return;
        
        BlockPos targetPos = findValidAnchorPosition();
        if (targetPos == null) {
            return;
        }
        
        anchorPos = targetPos;
        glowstonePos = anchorPos.up();  // Yarn: up()
        
        if (!mc.world.getBlockState(glowstonePos).isAir()) {
            return;
        }
        
        if (!hasItem(Items.RESPAWN_ANCHOR)) {
            mc.player.sendMessage(Text.literal("§c❌ No Respawn Anchor found!"), false);
            return;
        }
        
        if (!hasItem(Items.GLOWSTONE)) {
            mc.player.sendMessage(Text.literal("§c❌ No Glowstone found!"), false);
            return;
        }
        
        Vec3d eyePos = mc.player.getEyePos();  // Yarn: getEyePos()
        Vec3d targetCenter = Vec3d.ofCenter(anchorPos);
        float[] rotations = RotationManager.calculateRotationsTo(eyePos, targetCenter);
        
        if (silentRotations.getValue()) {
            RotationManager.setSmoothSpeed((float) rotationSpeed.getValue());
            RotationManager.setOnRotationComplete(() -> {
                rotationComplete = true;
            });
            RotationManager.setTargetRotation(rotations[0], rotations[1]);
        } else {
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(rotations[1]);
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
            return;
        }
        
        if (tickCounter > 40) {
            currentState = State.PLACING_ANCHOR;
        }
    }

    private void handlePlacingAnchor() {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;
        
        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.RESPAWN_ANCHOR);
            if (slot == -1) {
                mc.player.sendMessage(Text.literal("§c❌ No Respawn Anchor in hotbar!"), false);
                currentState = State.IDLE;
                return;
            }
            // Yarn: selectedSlot - folosim setter
            mc.player.getInventory().selectedSlot = slot;
        }
        
        boolean placed = placeBlock(anchorPos);
        
        if (placed) {
            if (visualFeedback.getValue()) {
                // Yarn: spawnBlockParticles
                mc.particleManager.spawnBlockParticles(anchorPos, mc.world.getBlockState(anchorPos));
            }
            
            mc.player.sendMessage(Text.literal("§a✅ Respawn Anchor placed!"), false);
            
            currentState = State.ROTATING_TO_GLOWSTONE;
            tickCounter = 0;
            rotationComplete = false;
            
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d targetCenter = Vec3d.ofCenter(glowstonePos);
            float[] rotations = RotationManager.calculateRotationsTo(eyePos, targetCenter);
            
            if (silentRotations.getValue()) {
                RotationManager.setSmoothSpeed((float) rotationSpeed.getValue());
                RotationManager.setOnRotationComplete(() -> {
                    rotationComplete = true;
                });
                RotationManager.setTargetRotation(rotations[0], rotations[1]);
            } else {
                mc.player.setYaw(rotations[0]);
                mc.player.setPitch(rotations[1]);
                rotationComplete = true;
            }
        } else {
            mc.player.sendMessage(Text.literal("§c❌ Failed to place Respawn Anchor!"), false);
            currentState = State.IDLE;
        }
    }

    private void handleRotatingToGlowstone() {
        if (rotationComplete || !silentRotations.getValue()) {
            currentState = State.PLACING_GLOWSTONE;
            rotationComplete = false;
            return;
        }
        
        if (tickCounter > 40) {
            currentState = State.PLACING_GLOWSTONE;
        }
    }

    private void handlePlacingGlowstone() {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;
        
        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.GLOWSTONE);
            if (slot == -1) {
                mc.player.sendMessage(Text.literal("§c❌ No Glowstone in hotbar!"), false);
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().selectedSlot = slot;
        }
        
        boolean placed = placeBlock(glowstonePos);
        
        if (placed) {
            if (visualFeedback.getValue()) {
                mc.particleManager.spawnBlockParticles(glowstonePos, mc.world.getBlockState(glowstonePos));
            }
            
            currentState = State.COMPLETE;
            RotationManager.clearRotation();
            
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(
                    "§a✅ Safe anchor placed at §e" + 
                    anchorPos.getX() + ", " + anchorPos.getY() + ", " + anchorPos.getZ()
                ), false);
            }
        } else {
            mc.player.sendMessage(Text.literal("§c❌ Failed to place Glowstone!"), false);
            currentState = State.IDLE;
        }
    }

    // ===== HELPER METHODS =====
    private boolean placeBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null) return false;
        if (mc.interactionManager == null) return false;
        
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        
        Direction dir = getPlaceDirection(pos);
        if (dir == null) return false;
        
        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(pos),
            dir,
            pos,
            false
        );
        
        // Yarn: interactBlock
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        
        return !mc.world.getBlockState(pos).isAir();
    }

    private Direction getPlaceDirection(BlockPos pos) {
        if (mc.player == null) return null;
        
        Vec3d playerPos = mc.player.getPos();  // Yarn: getPos()
        Vec3d blockPos = Vec3d.ofCenter(pos);
        Vec3d diff = blockPos.subtract(playerPos);
        
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
        
        BlockPos below = pos.down();  // Yarn: down()
        if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return false;
        }
        
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        
        BlockPos above = pos.up();  // Yarn: up()
        if (!mc.world.getBlockState(above).isAir()) {
            return false;
        }
        
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

    // ===== HUD INFO =====
    public String getHudInfo() {
        if (currentState == State.COMPLETE) {
            return "§aDone";
        } else if (currentState != State.IDLE) {
            return "§ePlacing...";
        }
        return null;
    }

    public int getHudInfoColor() {
        if (currentState == State.COMPLETE) {
            return 0xFF00FF00;
        } else if (currentState != State.IDLE) {
            return 0xFFFFFF00;
        }
        return 0xFFFFFFFF;
    }
}
