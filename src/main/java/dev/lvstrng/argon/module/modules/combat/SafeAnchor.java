package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.RotationManager;
import dev.lvstrng.argon.utils.RotationTickHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 0.3, 0.05, 1.0);
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
    private boolean isPlacing = false;

    // ===== CONSTRUCTOR =====
    public SafeAnchor() {
        super("Safe Anchor", "Places a safe respawn anchor with silent rotations", Category.COMBAT);
        addSettings(range, rotationSpeed, silentRotations, autoSwitch, visualFeedback);
        RotationTickHandler.register();

        // Register tick listener
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
        isPlacing = false;
        super.onEnable();
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§aSafe Anchor§7] §fEnabled"), true);
        }
    }

    @Override
    public void onDisable() {
        RotationManager.clearRotation();
        currentState = State.IDLE;
        isPlacing = false;
        super.onDisable();
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§aSafe Anchor§7] §fDisabled"), true);
        }
    }

    // ===== MAIN TICK LOGIC =====
    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.gameMode == null) return;

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
        
        // Găsește o poziție validă pentru anchor
        BlockPos targetPos = findValidAnchorPosition();
        if (targetPos == null) {
            // Nu s-a găsit poziție validă
            return;
        }
        
        anchorPos = targetPos;
        glowstonePos = anchorPos.up(); // above()
        
        // Verifică dacă glowstone-ul este liber
        if (!mc.world.getBlockState(glowstonePos).isAir()) {
            return;
        }
        
        // Verifică dacă ai itemele necesare
        if (!hasItem(Items.RESPAWN_ANCHOR)) {
            mc.player.sendMessage(Text.literal("§c❌ No Respawn Anchor found!"), true);
            return;
        }
        
        if (!hasItem(Items.GLOWSTONE)) {
            mc.player.sendMessage(Text.literal("§c❌ No Glowstone found!"), true);
            return;
        }
        
        // Calculează rotația către anchor
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetCenter = Vec3d.ofCenter(anchorPos);
        float[] rotations = RotationManager.calculateRotationsTo(eyePos, targetCenter);
        
        // Aplică rotația
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
        isPlacing = false;
    }

    private void handleRotatingToAnchor() {
        // Verifică dacă rotația s-a completat
        if (rotationComplete || !silentRotations.getValue()) {
            currentState = State.PLACING_ANCHOR;
            rotationComplete = false;
            return;
        }
        
        // Timeout după 2 secunde (40 tick-uri)
        if (tickCounter > 40) {
            currentState = State.PLACING_ANCHOR;
        }
    }

    private void handlePlacingAnchor() {
        if (mc.player == null || mc.world == null) return;
        if (mc.gameMode == null) return;
        
        // Selectează anchor-ul
        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.RESPAWN_ANCHOR);
            if (slot == -1) {
                mc.player.sendMessage(Text.literal("§c❌ No Respawn Anchor in hotbar!"), true);
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().selectedSlot = slot;
        }
        
        // Plasează anchor-ul
        boolean placed = placeBlock(anchorPos);
        
        if (placed) {
            // Feedback vizual
            if (visualFeedback.getValue()) {
                mc.particleManager.addBlockParticles(anchorPos, mc.world.getBlockState(anchorPos), 3);
            }
            
            mc.player.sendMessage(Text.literal("§a✅ Respawn Anchor placed!"), true);
            
            // Trecem la glowstone
            currentState = State.ROTATING_TO_GLOWSTONE;
            tickCounter = 0;
            rotationComplete = false;
            isPlacing = false;
            
            // Calculează rotația către glowstone
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
            // Plasare eșuată
            mc.player.sendMessage(Text.literal("§c❌ Failed to place Respawn Anchor!"), true);
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
        if (mc.gameMode == null) return;
        
        // Selectează glowstone-ul
        if (autoSwitch.getValue()) {
            int slot = findItemSlot(Items.GLOWSTONE);
            if (slot == -1) {
                mc.player.sendMessage(Text.literal("§c❌ No Glowstone in hotbar!"), true);
                currentState = State.IDLE;
                return;
            }
            mc.player.getInventory().selectedSlot = slot;
        }
        
        // Plasează glowstone-ul
        boolean placed = placeBlock(glowstonePos);
        
        if (placed) {
            // Feedback vizual
            if (visualFeedback.getValue()) {
                mc.particleManager.addBlockParticles(glowstonePos, mc.world.getBlockState(glowstonePos), 3);
            }
            
            currentState = State.COMPLETE;
            RotationManager.clearRotation();
            
            // Mesaj de confirmare
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(
                    "§a✅ Safe anchor placed at §e" + 
                    anchorPos.getX() + ", " + anchorPos.getY() + ", " + anchorPos.getZ()
                ), true);
            }
        } else {
            mc.player.sendMessage(Text.literal("§c❌ Failed to place Glowstone!"), true);
            currentState = State.IDLE;
        }
    }

    // ===== HELPER METHODS =====
    private boolean placeBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null) return false;
        if (mc.gameMode == null) return false;
        
        // Verifică dacă blocul este liber
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        
        // Găsește direcția din care să plaseze
        Direction dir = getPlaceDirection(pos);
        if (dir == null) return false;
        
        // Creează BlockHitResult
        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(pos),
            dir,
            pos,
            false
        );
        
        // Plasează blocul
        mc.gameMode.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        
        // Verifică dacă a fost plasat
        return !mc.world.getBlockState(pos).isAir();
    }

    private Direction getPlaceDirection(BlockPos pos) {
        if (mc.player == null) return null;
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d blockPos = Vec3d.ofCenter(pos);
        Vec3d diff = blockPos.subtract(playerPos);
        
        // Alege direcția opusă față de jucător
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
        
        // Caută în jurul jucătorului
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    
                    // Distanța trebuie să fie în range
                    double distSq = playerPos.getSquaredDistance(pos);
                    if (distSq > range.getValue() * range.getValue()) continue;
                    if (distSq < 1.0) continue;
                    
                    // Verifică dacă poziția e validă
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
        
        // Blocul de sub trebuie să fie solid
        BlockPos below = pos.down();
        if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return false;
        }
        
        // Poziția trebuie să fie aer
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        
        // Poziția de deasupra trebuie să fie aer (pentru glowstone)
        BlockPos above = pos.up();
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
    @Override
    public String getHudInfo() {
        if (currentState == State.COMPLETE) {
            return "§aDone";
        } else if (currentState != State.IDLE) {
            return "§ePlacing...";
        }
        return null;
    }

    @Override
    public int getHudInfoColor() {
        if (currentState == State.COMPLETE) {
            return 0xFF00FF00; // Verde
        } else if (currentState != State.IDLE) {
            return 0xFFFFFF00; // Galben
        }
        return super.getHudInfoColor();
    }
}
