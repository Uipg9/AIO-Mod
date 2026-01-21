package com.baesp.aio.rpg.economy;

import com.baesp.aio.AioMod;
import com.baesp.aio.network.AioNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/**
 * Shop Manager
 * 
 * Handles server-side shop transactions.
 * Uses ShopData for item definitions.
 */
public class ShopManager {
    
    public static void register() {
        AioMod.LOGGER.info("Shop Manager registered.");
    }
    
    /**
     * Process a buy request from a player
     * @param player The player buying the item
     * @param category Shop category index
     * @param itemIndex Item index within the category
     * @return true if purchase was successful
     */
    public static boolean buyItem(ServerPlayer player, int category, int itemIndex) {
        // Validate indices
        if (category < 0 || category >= ShopData.ITEMS.length) {
            player.sendSystemMessage(Component.literal("§cInvalid category!"));
            return false;
        }
        
        ShopData.ShopItem[] items = ShopData.ITEMS[category];
        if (itemIndex < 0 || itemIndex >= items.length) {
            player.sendSystemMessage(Component.literal("§cInvalid item!"));
            return false;
        }
        
        ShopData.ShopItem shopItem = items[itemIndex];
        
        // Check if player has enough money
        long playerMoney = EconomyManager.getMoney(player);
        if (playerMoney < shopItem.price) {
            long needed = shopItem.price - playerMoney;
            player.sendSystemMessage(Component.literal(
                "§cNot enough money! Need §e$" + formatNumber(needed) + " §cmore."
            ));
            return false;
        }
        
        // Get the item from registry
        Item item = getItemFromId(shopItem.itemId);
        if (item == null) {
            player.sendSystemMessage(Component.literal("§cItem not found: " + shopItem.itemId));
            AioMod.LOGGER.warn("Shop item not found: " + shopItem.itemId);
            return false;
        }
        
        // Create the item stack
        ItemStack stack = new ItemStack(item, shopItem.count);
        
        // Try to give item to player
        if (!player.getInventory().add(stack)) {
            // Inventory full, drop at feet
            player.drop(stack, false);
            player.sendSystemMessage(Component.literal("§7Inventory full - item dropped at your feet."));
        }
        
        // Deduct money
        EconomyManager.withdraw(player, shopItem.price);
        
        // Send success message
        String countText = shopItem.count > 1 ? " x" + shopItem.count : "";
        player.sendSystemMessage(Component.literal(
            "§aPurchased §f" + shopItem.name + countText + " §afor §e$" + formatNumber(shopItem.price)
        ));
        
        // Sync data back to client
        AioNetwork.sendSyncData(player);
        
        return true;
    }
    
    /**
     * Get an Item from its registry ID
     */
    private static Item getItemFromId(String itemId) {
        try {
            Identifier id = Identifier.parse(itemId);
            return BuiltInRegistries.ITEM.getValue(id);
        } catch (Exception e) {
            AioMod.LOGGER.error("Failed to parse item ID: " + itemId, e);
            return null;
        }
    }
    
    private static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
}
