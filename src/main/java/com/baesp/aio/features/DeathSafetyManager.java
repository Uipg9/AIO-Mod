package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Death Safety System
 * 
 * When a player dies, their inventory is saved and restored on respawn.
 * Optionally creates a "death chest" at the death location with overflow items.
 * 
 * Features:
 * - Inventory is saved on death
 * - Restored on respawn
 * - XP is partially preserved (50%)
 * - Death location is recorded and shown to player
 * - Optional death chest for extra protection
 * 
 * Inspired by Serilum's Keep Inventory and Gravestone mods.
 */
public class DeathSafetyManager {
    
    // Store saved inventories
    private static final Map<UUID, SavedInventory> savedInventories = new HashMap<>();
    
    // Store death locations for respawn message
    private static final Map<UUID, DeathLocation> deathLocations = new HashMap<>();
    
    public static void register() {
        // Check if death safety is enabled in config
        if (!AioMod.CONFIG.deathSafetyEnabled) {
            AioMod.LOGGER.info("Death Safety Manager disabled in config.");
            return;
        }
        
        // Handle death - save inventory
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            if (!AioMod.CONFIG.deathSafetyEnabled) return true; // Double-check config
            
            // Save the player's inventory before death
            saveInventory(player);
            
            // Record death location
            saveDeathLocation(player);
            
            return true; // Allow death to proceed
        });
        
        // Handle respawn - restore inventory
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!AioMod.CONFIG.deathSafetyEnabled) return; // Check config
            
            // Restore saved inventory
            restoreInventory(newPlayer);
            
            // Show death location
            showDeathLocation(newPlayer);
        });
        
        AioMod.LOGGER.info("Death Safety Manager registered.");
    }
    
    private static void saveInventory(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Save main inventory
        NonNullList<ItemStack> mainInv = NonNullList.withSize(36, ItemStack.EMPTY);
        for (int i = 0; i < 36; i++) {
            mainInv.set(i, player.getInventory().getItem(i).copy());
        }
        
        // Save armor (armor slots are 36-39 in inventory)
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) {
            armor.set(i, player.getInventory().getItem(36 + i).copy());
        }
        
        // Save offhand
        ItemStack offhand = player.getOffhandItem().copy();
        
        // Save XP
        int totalXp = calculateTotalXp(player);
        int keepXp = (int)(totalXp * AioMod.CONFIG.deathXpKeepPercent);
        
        savedInventories.put(playerId, new SavedInventory(mainInv, armor, offhand, keepXp));
        
        AioMod.LOGGER.debug("Saved inventory for player: " + player.getName().getString());
    }
    
    private static void restoreInventory(ServerPlayer player) {
        UUID playerId = player.getUUID();
        SavedInventory saved = savedInventories.remove(playerId);
        
        if (saved == null) return;
        
        // Restore main inventory
        for (int i = 0; i < 36 && i < saved.mainInventory.size(); i++) {
            player.getInventory().setItem(i, saved.mainInventory.get(i));
        }
        
        // Restore armor using armor slots (36-39)
        for (int i = 0; i < 4 && i < saved.armor.size(); i++) {
            player.getInventory().setItem(36 + i, saved.armor.get(i));
        }
        
        // Restore offhand (slot 40)
        player.getInventory().setItem(40, saved.offhand);
        
        // Restore XP
        player.giveExperiencePoints(saved.savedXp);
        
        // Notify player
        player.sendSystemMessage(Component.literal("§a✓ Your inventory has been restored!"));
        if (AioMod.CONFIG.deathXpKeepPercent < 1.0f) {
            player.sendSystemMessage(Component.literal("§7(" + (int)(AioMod.CONFIG.deathXpKeepPercent * 100) + "% of your XP was preserved)"));
        }
        
        // Play sound
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.2f);
        
        AioMod.LOGGER.debug("Restored inventory for player: " + player.getName().getString());
    }
    
    private static void saveDeathLocation(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        deathLocations.put(player.getUUID(), new DeathLocation(
            level.dimension().toString(),
            player.getBlockX(),
            player.getBlockY(),
            player.getBlockZ()
        ));
    }
    
    private static void showDeathLocation(ServerPlayer player) {
        DeathLocation loc = deathLocations.remove(player.getUUID());
        
        if (loc != null) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§c☠ You died at:"));
            player.sendSystemMessage(Component.literal("§7  Dimension: §f" + loc.dimension));
            player.sendSystemMessage(Component.literal("§7  Location: §e" + loc.x + ", " + loc.y + ", " + loc.z));
            player.sendSystemMessage(Component.literal(""));
        }
    }
    
    private static int calculateTotalXp(ServerPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        
        // Calculate total XP from levels
        int totalXp;
        if (level <= 16) {
            totalXp = (int)(level * level + 6 * level);
        } else if (level <= 31) {
            totalXp = (int)(2.5 * level * level - 40.5 * level + 360);
        } else {
            totalXp = (int)(4.5 * level * level - 162.5 * level + 2220);
        }
        
        // Add progress toward next level
        int xpForNextLevel = getXpForLevel(level);
        totalXp += (int)(xpForNextLevel * progress);
        
        return totalXp;
    }
    
    private static int getXpForLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
    
    /**
     * Alternative method: Create a death chest
     * Can be enabled instead of keep inventory
     */
    private static void createDeathChest(ServerPlayer player, ServerLevel level) {
        BlockPos deathPos = player.blockPosition();
        
        // Find a suitable position for the chest
        BlockPos chestPos = findChestPosition(level, deathPos);
        if (chestPos == null) return;
        
        // Place chest
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        
        // Get chest entity and fill it
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            int slot = 0;
            // Main inventory (0-35)
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && slot < 27) {
                    chest.setItem(slot++, stack.copy());
                }
            }
            // Armor (slots 36-39)
            for (int i = 0; i < 4; i++) {
                ItemStack stack = player.getInventory().getItem(36 + i);
                if (!stack.isEmpty() && slot < 27) {
                    chest.setItem(slot++, stack.copy());
                }
            }
            // Offhand
            if (!player.getOffhandItem().isEmpty() && slot < 27) {
                chest.setItem(slot, player.getOffhandItem().copy());
            }
        }
        
        AioMod.LOGGER.info("Created death chest for " + player.getName().getString() + " at " + chestPos);
    }
    
    private static BlockPos findChestPosition(ServerLevel level, BlockPos start) {
        // Try the death position first
        if (level.getBlockState(start).isAir()) {
            return start;
        }
        
        // Search nearby
        for (int y = 0; y <= 5; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos pos = start.offset(x, y, z);
                    if (level.getBlockState(pos).isAir() && 
                        level.getBlockState(pos.below()).isSolid()) {
                        return pos;
                    }
                }
            }
        }
        
        return null;
    }
    
    // Inner classes
    private record SavedInventory(
        NonNullList<ItemStack> mainInventory,
        NonNullList<ItemStack> armor,
        ItemStack offhand,
        int savedXp
    ) {}
    
    private record DeathLocation(String dimension, int x, int y, int z) {}
}
