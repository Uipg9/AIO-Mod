package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.network.chat.Component;

/**
 * Starter Kit System
 * 
 * Gives new players a starter kit on first join:
 * - Basic tools (stone tier)
 * - Some food
 * - Basic armor
 * - A welcome message
 * 
 * Inspired by Serilum's Starter Kit mod.
 */
public class StarterKitManager {
    
    public static void register() {
        // Give kit on first join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            // Check if player has received starter kit
            if (!hasReceivedKit(player)) {
                server.execute(() -> giveStarterKit(player));
            }
        });
        
        // Also handle respawn after first death (optional second chance kit)
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Could add respawn kit here if desired
        });
        
        AioMod.LOGGER.info("Starter Kit Manager registered.");
    }
    
    private static boolean hasReceivedKit(ServerPlayer player) {
        // Use player data to track if they've received the kit
        return PlayerDataManager.getBoolean(player, "received_starter_kit");
    }
    
    private static void setReceivedKit(ServerPlayer player) {
        PlayerDataManager.setBoolean(player, "received_starter_kit", true);
    }
    
    public static void giveStarterKit(ServerPlayer player) {
        giveStarterKit(player, true);
    }
    
    private static void giveStarterKit(ServerPlayer player, boolean markAsReceived) {
        // Mark as received first (only for new players)
        if (markAsReceived) {
            setReceivedKit(player);
        }
        
        // === TOOLS ===
        // Stone sword
        ItemStack sword = new ItemStack(Items.STONE_SWORD);
        giveItem(player, sword);
        
        // Stone pickaxe
        ItemStack pickaxe = new ItemStack(Items.STONE_PICKAXE);
        giveItem(player, pickaxe);
        
        // Stone axe
        ItemStack axe = new ItemStack(Items.STONE_AXE);
        giveItem(player, axe);
        
        // Stone shovel
        ItemStack shovel = new ItemStack(Items.STONE_SHOVEL);
        giveItem(player, shovel);
        
        // === ARMOR ===
        // Leather armor set - auto-equip to armor slots (36-39)
        player.getInventory().setItem(36, new ItemStack(Items.LEATHER_BOOTS));       // Feet slot (36)
        player.getInventory().setItem(37, new ItemStack(Items.LEATHER_LEGGINGS));    // Legs slot (37)
        player.getInventory().setItem(38, new ItemStack(Items.LEATHER_CHESTPLATE));  // Chest slot (38)
        player.getInventory().setItem(39, new ItemStack(Items.LEATHER_HELMET));      // Head slot (39)
        
        // === FOOD ===
        giveItem(player, new ItemStack(Items.BREAD, 16));
        giveItem(player, new ItemStack(Items.COOKED_BEEF, 8));
        
        // === UTILITY ===
        giveItem(player, new ItemStack(Items.TORCH, 32));
        giveItem(player, new ItemStack(Items.CRAFTING_TABLE));
        giveItem(player, new ItemStack(Items.FURNACE));
        giveItem(player, new ItemStack(Items.CHEST));
        giveItem(player, new ItemStack(Items.OAK_PLANKS, 32));
        
        // === MISC ===
        giveItem(player, new ItemStack(Items.COMPASS));
        giveItem(player, new ItemStack(Items.CLOCK));
        
        // Send welcome message
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6═══════════════════════════════════"));
        player.sendSystemMessage(Component.literal("§e    ⚔ §lWelcome to the Server! §e⚔"));
        player.sendSystemMessage(Component.literal("§6═══════════════════════════════════"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7You've received a §aStarter Kit§7!"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Key Features:"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eJ §fto open the §aShop"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eK §fto view your §bSkills"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eM §ffor §dAscendancy Tree"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eG §ffor §aSquat Grow"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eN §ffor §dVoid Magnet"));
        player.sendSystemMessage(Component.literal("§8• §fHold §eSHIFT §f+ break for Vein Mining"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eB §ffor Full Brightness"));
        player.sendSystemMessage(Component.literal("§8• §fPress §eH §fto toggle HUD sidebar"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Good luck and have fun!"));
        player.sendSystemMessage(Component.literal("§6═══════════════════════════════════"));
        player.sendSystemMessage(Component.literal(""));
        
        AioMod.LOGGER.info("Gave starter kit to new player: " + player.getName().getString());
    }
    
    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            // Inventory full, drop at player's feet
            player.drop(stack, false);
        }
    }
}
