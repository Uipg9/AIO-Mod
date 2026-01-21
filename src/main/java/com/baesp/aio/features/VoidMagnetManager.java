package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.rpg.economy.EconomyManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Void Magnet Feature
 * 
 * When enabled, pulls items toward the player.
 * "Junk" items are voided (deleted) in exchange for small currency amounts.
 * Valuable items are still collected normally.
 * 
 * Inspired by various item magnet mods.
 */
public class VoidMagnetManager {
    
    // Player UUID -> enabled state
    private static final Map<UUID, Boolean> enabledPlayers = new HashMap<>();
    
    // Items considered "junk" and their void values
    private static final Map<Item, Integer> JUNK_ITEMS = new HashMap<>();
    
    // Configuration
    private static final double MAGNET_RANGE = 8.0;
    private static final double PULL_SPEED = 0.3;
    private static final double VOID_RANGE = 1.5; // Items within this range get voided/collected
    
    static {
        // Initialize junk items with their void values
        // These give small amounts of money when voided
        JUNK_ITEMS.put(Items.DIRT, 1);
        JUNK_ITEMS.put(Items.COBBLESTONE, 1);
        JUNK_ITEMS.put(Items.STONE, 1);
        JUNK_ITEMS.put(Items.GRAVEL, 1);
        JUNK_ITEMS.put(Items.SAND, 1);
        JUNK_ITEMS.put(Items.NETHERRACK, 1);
        JUNK_ITEMS.put(Items.COBBLED_DEEPSLATE, 1);
        JUNK_ITEMS.put(Items.ANDESITE, 1);
        JUNK_ITEMS.put(Items.DIORITE, 1);
        JUNK_ITEMS.put(Items.GRANITE, 1);
        JUNK_ITEMS.put(Items.TUFF, 1);
        JUNK_ITEMS.put(Items.CALCITE, 1);
        JUNK_ITEMS.put(Items.DEEPSLATE, 1);
        JUNK_ITEMS.put(Items.ROTTEN_FLESH, 2);
        JUNK_ITEMS.put(Items.SPIDER_EYE, 2);
        JUNK_ITEMS.put(Items.STRING, 2);
        JUNK_ITEMS.put(Items.BONE, 2);
        JUNK_ITEMS.put(Items.GUNPOWDER, 3);
        JUNK_ITEMS.put(Items.ARROW, 1);
        JUNK_ITEMS.put(Items.POISONOUS_POTATO, 1);
        JUNK_ITEMS.put(Items.DEAD_BUSH, 1);
        JUNK_ITEMS.put(Items.FERN, 1);
        JUNK_ITEMS.put(Items.SHORT_GRASS, 1);
        JUNK_ITEMS.put(Items.TALL_GRASS, 1);
        JUNK_ITEMS.put(Items.SEAGRASS, 1);
        JUNK_ITEMS.put(Items.KELP, 1);
        JUNK_ITEMS.put(Items.WHEAT_SEEDS, 1);
        JUNK_ITEMS.put(Items.BEETROOT_SEEDS, 1);
        JUNK_ITEMS.put(Items.PUMPKIN_SEEDS, 1);
        JUNK_ITEMS.put(Items.MELON_SEEDS, 1);
        JUNK_ITEMS.put(Items.BAMBOO, 1);
        JUNK_ITEMS.put(Items.CACTUS, 1);
        JUNK_ITEMS.put(Items.SUGAR_CANE, 1);
        JUNK_ITEMS.put(Items.FLINT, 1);
        JUNK_ITEMS.put(Items.INK_SAC, 2);
        JUNK_ITEMS.put(Items.GLOW_INK_SAC, 3);
    }
    
    public static void register() {
        // Register server tick event for magnet functionality
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isEnabled(player)) {
                    tickMagnet(player);
                }
            }
        });
        
        AioMod.LOGGER.info("Void Magnet Manager registered.");
    }
    
    public static void setEnabled(ServerPlayer player, boolean enabled) {
        enabledPlayers.put(player.getUUID(), enabled);
    }
    
    public static boolean isEnabled(ServerPlayer player) {
        return enabledPlayers.getOrDefault(player.getUUID(), false);
    }
    
    public static void toggleEnabled(ServerPlayer player) {
        setEnabled(player, !isEnabled(player));
    }
    
    private static void tickMagnet(ServerPlayer player) {
        if (player.isSpectator() || player.isDeadOrDying()) return;
        
        ServerLevel level = (ServerLevel) player.level();
        Vec3 playerPos = player.position();
        
        // Find all items within range
        AABB searchBox = new AABB(
            playerPos.x - MAGNET_RANGE, playerPos.y - MAGNET_RANGE, playerPos.z - MAGNET_RANGE,
            playerPos.x + MAGNET_RANGE, playerPos.y + MAGNET_RANGE, playerPos.z + MAGNET_RANGE
        );
        
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox, 
            item -> !item.isRemoved() && item.isAlive() && !item.hasPickUpDelay());
        
        long totalVoidMoney = 0;
        int voidedCount = 0;
        
        for (ItemEntity itemEntity : items) {
            Vec3 itemPos = itemEntity.position();
            double distance = playerPos.distanceTo(itemPos);
            
            // Check if item is close enough to void/collect
            if (distance < VOID_RANGE) {
                Item item = itemEntity.getItem().getItem();
                int count = itemEntity.getItem().getCount();
                
                // Check if this is a junk item
                if (JUNK_ITEMS.containsKey(item)) {
                    int valuePerItem = JUNK_ITEMS.get(item);
                    long value = (long) valuePerItem * count;
                    totalVoidMoney += value;
                    voidedCount += count;
                    itemEntity.discard(); // Void the item
                }
                // Non-junk items are left for normal pickup
            } else {
                // Pull item toward player
                Vec3 direction = playerPos.subtract(itemPos).normalize();
                double pullStrength = PULL_SPEED * (1.0 - (distance / MAGNET_RANGE));
                
                Vec3 velocity = direction.scale(pullStrength);
                itemEntity.setDeltaMovement(
                    itemEntity.getDeltaMovement().add(velocity)
                );
            }
        }
        
        // Award voided item money
        if (totalVoidMoney > 0) {
            EconomyManager.deposit(player, totalVoidMoney);
            // The floating message will be triggered by the data sync
        }
    }
    
    /**
     * Check if an item is considered junk
     */
    public static boolean isJunkItem(Item item) {
        return JUNK_ITEMS.containsKey(item);
    }
    
    /**
     * Get the void value of an item
     */
    public static int getVoidValue(Item item) {
        return JUNK_ITEMS.getOrDefault(item, 0);
    }
    
    /**
     * Add a custom junk item (for config/expansion)
     */
    public static void addJunkItem(Item item, int value) {
        JUNK_ITEMS.put(item, value);
    }
}
