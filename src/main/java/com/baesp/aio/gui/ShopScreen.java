package com.baesp.aio.gui;

import com.baesp.aio.rpg.economy.EconomyManager;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends SimpleGui {
    private final ServerPlayer player;
    private int currentPage = 0;
    private final List<ShopItem> shopItems;
    
    public ShopScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.shopItems = createShopItems();
        
        setTitle(Component.literal("§6✦ Shop ✦"));
        setupGui();
    }
    
    private List<ShopItem> createShopItems() {
        List<ShopItem> items = new ArrayList<>();
        
        // Tools
        items.add(new ShopItem(Items.WOODEN_PICKAXE.getDefaultInstance(), 10, "Wooden Pickaxe"));
        items.add(new ShopItem(Items.STONE_PICKAXE.getDefaultInstance(), 25, "Stone Pickaxe"));
        items.add(new ShopItem(Items.IRON_PICKAXE.getDefaultInstance(), 100, "Iron Pickaxe"));
        items.add(new ShopItem(Items.DIAMOND_PICKAXE.getDefaultInstance(), 500, "Diamond Pickaxe"));
        
        items.add(new ShopItem(Items.WOODEN_SWORD.getDefaultInstance(), 10, "Wooden Sword"));
        items.add(new ShopItem(Items.STONE_SWORD.getDefaultInstance(), 25, "Stone Sword"));
        items.add(new ShopItem(Items.IRON_SWORD.getDefaultInstance(), 100, "Iron Sword"));
        items.add(new ShopItem(Items.DIAMOND_SWORD.getDefaultInstance(), 500, "Diamond Sword"));
        
        // Armor
        items.add(new ShopItem(Items.LEATHER_HELMET.getDefaultInstance(), 20, "Leather Helmet"));
        items.add(new ShopItem(Items.LEATHER_CHESTPLATE.getDefaultInstance(), 40, "Leather Chestplate"));
        items.add(new ShopItem(Items.LEATHER_LEGGINGS.getDefaultInstance(), 35, "Leather Leggings"));
        items.add(new ShopItem(Items.LEATHER_BOOTS.getDefaultInstance(), 20, "Leather Boots"));
        
        items.add(new ShopItem(Items.IRON_HELMET.getDefaultInstance(), 80, "Iron Helmet"));
        items.add(new ShopItem(Items.IRON_CHESTPLATE.getDefaultInstance(), 160, "Iron Chestplate"));
        items.add(new ShopItem(Items.IRON_LEGGINGS.getDefaultInstance(), 140, "Iron Leggings"));
        items.add(new ShopItem(Items.IRON_BOOTS.getDefaultInstance(), 80, "Iron Boots"));
        
        items.add(new ShopItem(Items.DIAMOND_HELMET.getDefaultInstance(), 400, "Diamond Helmet"));
        items.add(new ShopItem(Items.DIAMOND_CHESTPLATE.getDefaultInstance(), 800, "Diamond Chestplate"));
        items.add(new ShopItem(Items.DIAMOND_LEGGINGS.getDefaultInstance(), 700, "Diamond Leggings"));
        items.add(new ShopItem(Items.DIAMOND_BOOTS.getDefaultInstance(), 400, "Diamond Boots"));
        
        // Food
        items.add(new ShopItem(new ItemStack(Items.BREAD, 16), 15, "Bread x16"));
        items.add(new ShopItem(new ItemStack(Items.COOKED_BEEF, 16), 30, "Steak x16"));
        items.add(new ShopItem(new ItemStack(Items.GOLDEN_APPLE, 1), 200, "Golden Apple"));
        items.add(new ShopItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), 5000, "Enchanted Golden Apple"));
        
        // Building
        items.add(new ShopItem(new ItemStack(Items.OAK_LOG, 64), 50, "Oak Logs x64"));
        items.add(new ShopItem(new ItemStack(Items.COBBLESTONE, 64), 30, "Cobblestone x64"));
        items.add(new ShopItem(new ItemStack(Items.IRON_INGOT, 16), 80, "Iron Ingots x16"));
        items.add(new ShopItem(new ItemStack(Items.GOLD_INGOT, 16), 120, "Gold Ingots x16"));
        items.add(new ShopItem(new ItemStack(Items.DIAMOND, 4), 200, "Diamonds x4"));
        
        // Utility
        items.add(new ShopItem(new ItemStack(Items.TORCH, 64), 20, "Torches x64"));
        items.add(new ShopItem(new ItemStack(Items.ENDER_PEARL, 4), 100, "Ender Pearls x4"));
        items.add(new ShopItem(new ItemStack(Items.EXPERIENCE_BOTTLE, 16), 150, "XP Bottles x16"));
        
        return items;
    }
    
    private void setupGui() {
        // Clear all slots
        for (int i = 0; i < 54; i++) {
            setSlot(i, new GuiElementBuilder()
                .setItem(Items.BLACK_STAINED_GLASS_PANE)
                .setName(Component.literal(""))
                .build()
            );
        }
        
        // Gold accent corners
        for (int slot : new int[]{0, 8, 45, 53}) {
            setSlot(slot, new GuiElementBuilder()
                .setItem(Items.GOLD_BLOCK)
                .setName(Component.literal(""))
                .build()
            );
        }
        
        // Balance display
        long balance = EconomyManager.getMoney(player);
        setSlot(4, new GuiElementBuilder()
            .setItem(Items.GOLD_INGOT)
            .setName(Component.literal("§6Balance: §e$" + EconomyManager.formatMoney(balance)))
            .addLoreLine(Component.literal("§7Click items below to buy!"))
            .build()
        );
        
        // Shop items (slots 10-16, 19-25, 28-34, 37-43)
        int[] itemSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int startIndex = currentPage * itemSlots.length;
        
        for (int i = 0; i < itemSlots.length; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < shopItems.size()) {
                ShopItem shopItem = shopItems.get(itemIndex);
                boolean canAfford = EconomyManager.canAfford(player, shopItem.price);
                
                setSlot(itemSlots[i], new GuiElementBuilder()
                    .setItem(shopItem.item.getItem())
                    .setCount(shopItem.item.getCount())
                    .setName(Component.literal((canAfford ? "§a" : "§c") + shopItem.name))
                    .addLoreLine(Component.literal(""))
                    .addLoreLine(Component.literal("§7Price: §e$" + EconomyManager.formatMoney(shopItem.price)))
                    .addLoreLine(Component.literal(""))
                    .addLoreLine(canAfford 
                        ? Component.literal("§aClick to buy!") 
                        : Component.literal("§cCannot afford!"))
                    .setCallback((index, type, action) -> {
                        if (EconomyManager.withdraw(player, shopItem.price)) {
                            player.getInventory().add(shopItem.item.copy());
                            player.sendSystemMessage(Component.literal("§aPurchased " + shopItem.name + "!"));
                            setupGui(); // Refresh balance
                        } else {
                            player.sendSystemMessage(Component.literal("§cInsufficient funds!"));
                        }
                    })
                    .build()
                );
            }
        }
        
        // Navigation - Previous page
        if (currentPage > 0) {
            setSlot(45, new GuiElementBuilder()
                .setItem(Items.ARROW)
                .setName(Component.literal("§7← Previous Page"))
                .setCallback((index, type, action) -> {
                    currentPage--;
                    setupGui();
                })
                .build()
            );
        }
        
        // Navigation - Next page
        int maxPage = (shopItems.size() - 1) / itemSlots.length;
        if (currentPage < maxPage) {
            setSlot(53, new GuiElementBuilder()
                .setItem(Items.ARROW)
                .setName(Component.literal("§7Next Page →"))
                .setCallback((index, type, action) -> {
                    currentPage++;
                    setupGui();
                })
                .build()
            );
        }
        
        // Close button
        setSlot(49, new GuiElementBuilder()
            .setItem(Items.BARRIER)
            .setName(Component.literal("§cClose"))
            .setCallback((index, type, action) -> close())
            .build()
        );
    }
    
    public static void open(ServerPlayer player) {
        new ShopScreen(player).open();
    }
    
    private record ShopItem(ItemStack item, long price, String name) {}
}
