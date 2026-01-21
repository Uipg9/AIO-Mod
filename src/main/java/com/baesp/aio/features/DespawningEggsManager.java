package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Despawning Eggs Hatch System
 * 
 * When eggs are about to despawn (been on ground for too long),
 * they hatch into chickens instead of disappearing.
 * 
 * Features:
 * - Eggs near despawn time hatch into chicks
 * - Small chance for multiple chicks (like thrown eggs)
 * - Works with eggs dropped by chickens or players
 * - Prevents egg waste
 * 
 * Inspired by Serilum's Despawning Eggs Hatch mod.
 */
public class DespawningEggsManager {
    
    // Check interval in ticks
    private static final int CHECK_INTERVAL = 200; // Every 10 seconds
    
    // Eggs despawn after 6000 ticks (5 minutes)
    // We check eggs that are close to despawning (within 30 seconds)
    private static final int DESPAWN_THRESHOLD = 5400; // 4.5 minutes old
    
    // Chance for each egg to hatch (like throwing)
    private static final double HATCH_CHANCE = 0.125; // 1/8 like normal eggs
    private static final double MULTI_CHICK_CHANCE = 0.03125; // 1/32 for 4 chicks
    
    private static int tickCounter = 0;
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            
            if (tickCounter >= CHECK_INTERVAL) {
                tickCounter = 0;
                
                for (ServerLevel level : server.getAllLevels()) {
                    processEggs(level);
                }
            }
        });
        
        AioMod.LOGGER.info("Despawning Eggs Hatch Manager registered.");
    }
    
    private static void processEggs(ServerLevel level) {
        // Find all item entities that are eggs
        List<ItemEntity> eggsToProcess = new ArrayList<>();
        
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();
                if (stack.is(Items.EGG)) {
                    int age = itemEntity.getAge();
                    if (age >= DESPAWN_THRESHOLD) {
                        eggsToProcess.add(itemEntity);
                    }
                }
            }
        }
        
        for (ItemEntity eggEntity : eggsToProcess) {
            tryHatchEgg(level, eggEntity);
        }
    }
    
    private static void tryHatchEgg(ServerLevel level, ItemEntity eggEntity) {
        ItemStack stack = eggEntity.getItem();
        int count = stack.getCount();
        
        // Process each egg in the stack
        int totalChicks = 0;
        for (int i = 0; i < count; i++) {
            if (level.random.nextDouble() < HATCH_CHANCE) {
                // Egg hatches!
                if (level.random.nextDouble() < MULTI_CHICK_CHANCE) {
                    totalChicks += 4; // Rare: 4 chicks
                } else {
                    totalChicks += 1;
                }
            }
        }
        
        // Spawn the chicks using the new API
        if (totalChicks > 0) {
            for (int i = 0; i < totalChicks; i++) {
                Entity chick = EntityType.CHICKEN.create(level, EntitySpawnReason.BREEDING);
                if (chick != null) {
                    // Spawn at egg location with slight offset
                    double x = eggEntity.getX() + (level.random.nextDouble() - 0.5) * 0.5;
                    double y = eggEntity.getY();
                    double z = eggEntity.getZ() + (level.random.nextDouble() - 0.5) * 0.5;
                    
                    chick.setPos(x, y, z);
                    chick.setYRot(level.random.nextFloat() * 360.0f);
                    
                    level.addFreshEntity(chick);
                }
            }
            
            AioMod.LOGGER.debug("Hatched " + totalChicks + " chick(s) from despawning eggs at " + 
                eggEntity.blockPosition());
        }
        
        // Remove the eggs
        eggEntity.discard();
    }
}
