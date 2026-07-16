package dev.lvstrng.argon.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents; // Correct import for Fabric API
import net.minecraft.client.MinecraftClient;

public class RotationTickHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RotationManager.update(1.0f);
        });
        System.out.println("[RotationManager] Tick handler registered!");
    }
}
