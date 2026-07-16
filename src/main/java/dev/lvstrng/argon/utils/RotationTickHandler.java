package dlindustries.vigillant.system.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class RotationTickHandler {
    
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
        
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            // Actualizează RotationManager la fiecare tick
            RotationManager.update(1.0f);
        });
        
        System.out.println("[RotationManager] Tick handler registered!");
    }
}
