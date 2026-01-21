package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.network.chat.Component;

/**
 * Pet Names System
 * 
 * Shows custom name tags above tamed pets automatically.
 * 
 * Features:
 * - Tamed animals show their owner's name if not custom named
 * - Custom names are always visible (like name tags)
 * - Wolves, cats, parrots all supported
 * 
 * Inspired by Serilum's Pet Names and similar mods.
 */
public class PetNamesManager {
    
    // Check interval in ticks
    private static final int CHECK_INTERVAL = 100; // Every 5 seconds
    private static int tickCounter = 0;
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            
            if (tickCounter >= CHECK_INTERVAL) {
                tickCounter = 0;
                
                // Process all worlds
                for (ServerLevel level : server.getAllLevels()) {
                    updatePetVisibility(level);
                }
            }
        });
        
        AioMod.LOGGER.info("Pet Names Manager registered.");
    }
    
    private static void updatePetVisibility(ServerLevel level) {
        // Iterate all entities and check if they're tamable and tamed
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof TamableAnimal pet && pet.isTame()) {
                ensureNameVisible(pet);
            }
        }
    }
    
    private static void ensureNameVisible(TamableAnimal pet) {
        // If pet has a custom name, make it always visible
        if (pet.hasCustomName()) {
            pet.setCustomNameVisible(true);
        } else if (pet.getOwner() != null) {
            // Set a default name based on owner
            String ownerName = pet.getOwner().getName().getString();
            String petType = pet.getType().getDescriptionId();
            
            // Extract simple type name from translation key
            String simpleName = "Pet";
            if (petType.contains("wolf")) simpleName = "Wolf";
            else if (petType.contains("cat")) simpleName = "Cat";
            else if (petType.contains("parrot")) simpleName = "Parrot";
            
            // Set name like "Steve's Wolf"
            pet.setCustomName(Component.literal("§7" + ownerName + "'s " + simpleName));
            pet.setCustomNameVisible(true);
        }
    }
}
