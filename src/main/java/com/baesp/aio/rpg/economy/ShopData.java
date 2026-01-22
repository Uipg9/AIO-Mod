package com.baesp.aio.rpg.economy;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop inventory data - Contains all purchasable items organized by category
 * Expanded with rare materials, spawn eggs, utility blocks, and more
 */
public class ShopData {
    
    // Categories
    public static final String[] CATEGORIES = {
        "Tools", "Armor", "Weapons", "Food", "Farming", "Blocks", 
        "Ores & Gems", "Spawn Eggs", "Utility", "Enchanted", "Rare"
    };
    
    public static final String[] CATEGORY_ICONS = {
        "§6⛏", "§9🛡", "§c⚔", "§a🍎", "§2⚘", "§e⬛", 
        "§b💎", "§d🥚", "§f⚡", "§5✨", "§6★"
    };
    
    public static final int[] CATEGORY_COLORS = {
        0xFFFFAA00, // Tools - Orange
        0xFF5555FF, // Armor - Blue
        0xFFFF5555, // Weapons - Red
        0xFF55FF55, // Food - Green
        0xFF00AA00, // Farming - Dark Green
        0xFFFFFF55, // Blocks - Yellow
        0xFF55FFFF, // Ores - Cyan
        0xFFFF55FF, // Spawn Eggs - Magenta
        0xFFFFFFFF, // Utility - White
        0xFFAA00FF, // Enchanted - Purple
        0xFFFFAA00  // Rare - Gold
    };
    
    // Shop items per category
    public static final ShopItem[][] ITEMS = {
        // Tools
        {
            new ShopItem("Iron Pickaxe", "minecraft:iron_pickaxe", 50, "§7⛏"),
            new ShopItem("Iron Axe", "minecraft:iron_axe", 50, "§7🪓"),
            new ShopItem("Iron Shovel", "minecraft:iron_shovel", 40, "§7⚒"),
            new ShopItem("Iron Hoe", "minecraft:iron_hoe", 30, "§7⌇"),
            new ShopItem("Diamond Pickaxe", "minecraft:diamond_pickaxe", 300, "§b⛏"),
            new ShopItem("Diamond Axe", "minecraft:diamond_axe", 300, "§b🪓"),
            new ShopItem("Diamond Shovel", "minecraft:diamond_shovel", 250, "§b⚒"),
            new ShopItem("Diamond Hoe", "minecraft:diamond_hoe", 200, "§b⌇"),
            new ShopItem("Netherite Pickaxe", "minecraft:netherite_pickaxe", 1500, "§8⛏"),
            new ShopItem("Netherite Axe", "minecraft:netherite_axe", 1500, "§8🪓"),
            new ShopItem("Netherite Shovel", "minecraft:netherite_shovel", 1200, "§8⚒"),
            new ShopItem("Netherite Hoe", "minecraft:netherite_hoe", 1000, "§8⌇"),
            new ShopItem("Shears", "minecraft:shears", 25, "§f✂"),
            new ShopItem("Flint and Steel", "minecraft:flint_and_steel", 30, "§6🔥"),
            new ShopItem("Fishing Rod", "minecraft:fishing_rod", 40, "§6🎣"),
            new ShopItem("Brush", "minecraft:brush", 100, "§e🖌")
        },
        // Armor
        {
            new ShopItem("Iron Helmet", "minecraft:iron_helmet", 80, "§7⛑"),
            new ShopItem("Iron Chestplate", "minecraft:iron_chestplate", 120, "§7🎽"),
            new ShopItem("Iron Leggings", "minecraft:iron_leggings", 100, "§7👖"),
            new ShopItem("Iron Boots", "minecraft:iron_boots", 60, "§7👢"),
            new ShopItem("Diamond Helmet", "minecraft:diamond_helmet", 400, "§b⛑"),
            new ShopItem("Diamond Chestplate", "minecraft:diamond_chestplate", 600, "§b🎽"),
            new ShopItem("Diamond Leggings", "minecraft:diamond_leggings", 500, "§b👖"),
            new ShopItem("Diamond Boots", "minecraft:diamond_boots", 300, "§b👢"),
            new ShopItem("Netherite Helmet", "minecraft:netherite_helmet", 2000, "§8⛑"),
            new ShopItem("Netherite Chestplate", "minecraft:netherite_chestplate", 3000, "§8🎽"),
            new ShopItem("Netherite Leggings", "minecraft:netherite_leggings", 2500, "§8👖"),
            new ShopItem("Netherite Boots", "minecraft:netherite_boots", 1500, "§8👢"),
            new ShopItem("Shield", "minecraft:shield", 100, "§f🛡"),
            new ShopItem("Turtle Shell", "minecraft:turtle_helmet", 500, "§a⛑")
        },
        // Weapons
        {
            new ShopItem("Iron Sword", "minecraft:iron_sword", 100, "§7⚔"),
            new ShopItem("Diamond Sword", "minecraft:diamond_sword", 500, "§b⚔"),
            new ShopItem("Netherite Sword", "minecraft:netherite_sword", 2000, "§8⚔"),
            new ShopItem("Bow", "minecraft:bow", 150, "§6🏹"),
            new ShopItem("Crossbow", "minecraft:crossbow", 200, "§6⚔"),
            new ShopItem("Arrow x64", "minecraft:arrow", 50, "§f➤", 64),
            new ShopItem("Spectral Arrow x16", "minecraft:spectral_arrow", 100, "§e➤", 16),
            new ShopItem("Tipped Arrow (Poison) x8", "minecraft:tipped_arrow", 80, "§2➤", 8),
            new ShopItem("Trident", "minecraft:trident", 5000, "§b🔱"),
            new ShopItem("Mace", "minecraft:mace", 8000, "§6🔨")
        },
        // Food
        {
            new ShopItem("Bread x16", "minecraft:bread", 20, "§e🍞", 16),
            new ShopItem("Cooked Beef x16", "minecraft:cooked_beef", 40, "§6🥩", 16),
            new ShopItem("Cooked Porkchop x16", "minecraft:cooked_porkchop", 40, "§6🥓", 16),
            new ShopItem("Cooked Chicken x16", "minecraft:cooked_chicken", 30, "§f🍗", 16),
            new ShopItem("Baked Potato x16", "minecraft:baked_potato", 25, "§e🥔", 16),
            new ShopItem("Golden Carrot x8", "minecraft:golden_carrot", 100, "§6🥕", 8),
            new ShopItem("Golden Apple", "minecraft:golden_apple", 500, "§e🍎"),
            new ShopItem("Enchanted Golden Apple", "minecraft:enchanted_golden_apple", 5000, "§d🍎"),
            new ShopItem("Cake", "minecraft:cake", 75, "§f🎂"),
            new ShopItem("Pumpkin Pie x8", "minecraft:pumpkin_pie", 50, "§6🥧", 8),
            new ShopItem("Honey Bottle x4", "minecraft:honey_bottle", 40, "§6🍯", 4),
            new ShopItem("Suspicious Stew", "minecraft:suspicious_stew", 100, "§d🍲")
        },
        // Farming
        {
            new ShopItem("Wheat Seeds x32", "minecraft:wheat_seeds", 10, "§e🌱", 32),
            new ShopItem("Beetroot Seeds x32", "minecraft:beetroot_seeds", 15, "§c🌱", 32),
            new ShopItem("Melon Seeds x16", "minecraft:melon_seeds", 20, "§a🌱", 16),
            new ShopItem("Pumpkin Seeds x16", "minecraft:pumpkin_seeds", 20, "§6🌱", 16),
            new ShopItem("Carrot x16", "minecraft:carrot", 25, "§6🥕", 16),
            new ShopItem("Potato x16", "minecraft:potato", 25, "§e🥔", 16),
            new ShopItem("Bone Meal x32", "minecraft:bone_meal", 30, "§f⬤", 32),
            new ShopItem("Composter", "minecraft:composter", 30, "§6🗑"),
            new ShopItem("Hay Bale x16", "minecraft:hay_block", 50, "§e◼", 16),
            new ShopItem("Oak Sapling x16", "minecraft:oak_sapling", 20, "§2🌲", 16),
            new ShopItem("Spruce Sapling x16", "minecraft:spruce_sapling", 20, "§4🌲", 16),
            new ShopItem("Birch Sapling x16", "minecraft:birch_sapling", 20, "§f🌲", 16),
            new ShopItem("Jungle Sapling x16", "minecraft:jungle_sapling", 30, "§2🌲", 16),
            new ShopItem("Dark Oak Sapling x16", "minecraft:dark_oak_sapling", 30, "§8🌲", 16),
            new ShopItem("Acacia Sapling x16", "minecraft:acacia_sapling", 25, "§6🌲", 16),
            new ShopItem("Cherry Sapling x8", "minecraft:cherry_sapling", 50, "§d🌲", 8),
            new ShopItem("Mangrove Propagule x8", "minecraft:mangrove_propagule", 40, "§2🌱", 8),
            new ShopItem("Cocoa Beans x16", "minecraft:cocoa_beans", 30, "§6⬤", 16),
            new ShopItem("Sugar Cane x16", "minecraft:sugar_cane", 25, "§a|", 16),
            new ShopItem("Bamboo x32", "minecraft:bamboo", 20, "§2|", 32),
            new ShopItem("Cactus x8", "minecraft:cactus", 25, "§2🌵", 8),
            new ShopItem("Sweet Berries x16", "minecraft:sweet_berries", 30, "§c⬤", 16),
            new ShopItem("Glow Berries x8", "minecraft:glow_berries", 50, "§e✦", 8),
            new ShopItem("Nether Wart x16", "minecraft:nether_wart", 80, "§4⬤", 16)
        },
        // Blocks
        {
            new ShopItem("Oak Log x64", "minecraft:oak_log", 30, "§6🪵", 64),
            new ShopItem("Spruce Log x64", "minecraft:spruce_log", 30, "§4🪵", 64),
            new ShopItem("Birch Log x64", "minecraft:birch_log", 30, "§f🪵", 64),
            new ShopItem("Stone x64", "minecraft:stone", 20, "§7◻", 64),
            new ShopItem("Cobblestone x64", "minecraft:cobblestone", 10, "§7◼", 64),
            new ShopItem("Deepslate x64", "minecraft:deepslate", 25, "§8◼", 64),
            new ShopItem("Glass x64", "minecraft:glass", 30, "§f◻", 64),
            new ShopItem("Quartz Block x32", "minecraft:quartz_block", 100, "§f◻", 32),
            new ShopItem("Obsidian x16", "minecraft:obsidian", 200, "§5◼", 16),
            new ShopItem("Crying Obsidian x8", "minecraft:crying_obsidian", 300, "§d◼", 8),
            new ShopItem("Glowstone x32", "minecraft:glowstone", 100, "§e✦", 32),
            new ShopItem("Sea Lantern x16", "minecraft:sea_lantern", 150, "§b✦", 16),
            new ShopItem("Packed Ice x32", "minecraft:packed_ice", 80, "§b◻", 32),
            new ShopItem("Blue Ice x16", "minecraft:blue_ice", 200, "§9◻", 16)
        },
        // Ores & Gems
        {
            new ShopItem("Coal x64", "minecraft:coal", 30, "§8⬤", 64),
            new ShopItem("Raw Iron x32", "minecraft:raw_iron", 60, "§f⬤", 32),
            new ShopItem("Raw Gold x16", "minecraft:raw_gold", 100, "§e⬤", 16),
            new ShopItem("Raw Copper x64", "minecraft:raw_copper", 40, "§6⬤", 64),
            new ShopItem("Iron Ingot x32", "minecraft:iron_ingot", 100, "§f⬛", 32),
            new ShopItem("Gold Ingot x16", "minecraft:gold_ingot", 150, "§e⬛", 16),
            new ShopItem("Copper Ingot x32", "minecraft:copper_ingot", 60, "§6⬛", 32),
            new ShopItem("Diamond x8", "minecraft:diamond", 400, "§b💎", 8),
            new ShopItem("Emerald x8", "minecraft:emerald", 300, "§a💎", 8),
            new ShopItem("Lapis Lazuli x32", "minecraft:lapis_lazuli", 80, "§9⬤", 32),
            new ShopItem("Redstone x64", "minecraft:redstone", 50, "§c⬤", 64),
            new ShopItem("Amethyst Shard x16", "minecraft:amethyst_shard", 100, "§d💎", 16),
            new ShopItem("Netherite Scrap", "minecraft:netherite_scrap", 1000, "§8⬛"),
            new ShopItem("Netherite Ingot", "minecraft:netherite_ingot", 5000, "§8⬛"),
            new ShopItem("Ancient Debris", "minecraft:ancient_debris", 800, "§4⬛")
        },
        // Spawn Eggs
        {
            new ShopItem("Chicken Spawn Egg", "minecraft:chicken_spawn_egg", 100, "§f🥚"),
            new ShopItem("Cow Spawn Egg", "minecraft:cow_spawn_egg", 150, "§6🥚"),
            new ShopItem("Pig Spawn Egg", "minecraft:pig_spawn_egg", 150, "§d🥚"),
            new ShopItem("Sheep Spawn Egg", "minecraft:sheep_spawn_egg", 150, "§f🥚"),
            new ShopItem("Wolf Spawn Egg", "minecraft:wolf_spawn_egg", 500, "§7🥚"),
            new ShopItem("Cat Spawn Egg", "minecraft:cat_spawn_egg", 500, "§6🥚"),
            new ShopItem("Horse Spawn Egg", "minecraft:horse_spawn_egg", 800, "§6🥚"),
            new ShopItem("Donkey Spawn Egg", "minecraft:donkey_spawn_egg", 600, "§7🥚"),
            new ShopItem("Llama Spawn Egg", "minecraft:llama_spawn_egg", 400, "§f🥚"),
            new ShopItem("Parrot Spawn Egg", "minecraft:parrot_spawn_egg", 700, "§c🥚"),
            new ShopItem("Axolotl Spawn Egg", "minecraft:axolotl_spawn_egg", 1000, "§d🥚"),
            new ShopItem("Bee Spawn Egg", "minecraft:bee_spawn_egg", 600, "§e🥚"),
            new ShopItem("Fox Spawn Egg", "minecraft:fox_spawn_egg", 500, "§6🥚"),
            new ShopItem("Rabbit Spawn Egg", "minecraft:rabbit_spawn_egg", 200, "§f🥚"),
            new ShopItem("Villager Spawn Egg", "minecraft:villager_spawn_egg", 2000, "§6🥚"),
            new ShopItem("Iron Golem Spawn Egg", "minecraft:iron_golem_spawn_egg", 5000, "§f🥚")
        },
        // Utility
        {
            new ShopItem("Ender Pearl x8", "minecraft:ender_pearl", 200, "§5⬤", 8),
            new ShopItem("Ender Eye x4", "minecraft:ender_eye", 500, "§a👁", 4),
            new ShopItem("Blaze Rod x8", "minecraft:blaze_rod", 150, "§6|", 8),
            new ShopItem("Blaze Powder x16", "minecraft:blaze_powder", 100, "§6✦", 16),
            new ShopItem("Ghast Tear x4", "minecraft:ghast_tear", 300, "§f💧", 4),
            new ShopItem("Phantom Membrane x8", "minecraft:phantom_membrane", 200, "§7◇", 8),
            new ShopItem("Nether Wart x16", "minecraft:nether_wart", 80, "§4⬤", 16),
            new ShopItem("Slime Ball x16", "minecraft:slime_ball", 60, "§a⬤", 16),
            new ShopItem("Magma Cream x8", "minecraft:magma_cream", 100, "§6⬤", 8),
            new ShopItem("Gunpowder x32", "minecraft:gunpowder", 80, "§7⬤", 32),
            new ShopItem("String x32", "minecraft:string", 40, "§f~", 32),
            new ShopItem("Leather x16", "minecraft:leather", 50, "§6◼", 16),
            new ShopItem("Saddle", "minecraft:saddle", 300, "§6🪑"),
            new ShopItem("Name Tag x4", "minecraft:name_tag", 200, "§f📛", 4),
            new ShopItem("Lead x4", "minecraft:lead", 100, "§e~", 4),
            new ShopItem("Bucket", "minecraft:bucket", 30, "§7🪣"),
            new ShopItem("Water Bucket", "minecraft:water_bucket", 50, "§9🪣"),
            new ShopItem("Lava Bucket", "minecraft:lava_bucket", 100, "§6🪣")
        },
        // Enchanted Items
        {
            new ShopItem("Bottle o' Enchanting x16", "minecraft:experience_bottle", 200, "§a⚗", 16),
            new ShopItem("Enchanting Table", "minecraft:enchanting_table", 1000, "§5📖"),
            new ShopItem("Anvil", "minecraft:anvil", 500, "§7⬛"),
            new ShopItem("Grindstone", "minecraft:grindstone", 200, "§7⚙"),
            new ShopItem("Bookshelf x16", "minecraft:bookshelf", 200, "§6📚", 16),
            new ShopItem("Book x16", "minecraft:book", 50, "§6📕", 16),
            new ShopItem("Enchanted Book (Mending)", "minecraft:enchanted_book", 3000, "§5📖"),
            new ShopItem("Enchanted Book (Unbreaking III)", "minecraft:enchanted_book", 1500, "§5📖"),
            new ShopItem("Enchanted Book (Fortune III)", "minecraft:enchanted_book", 2500, "§5📖"),
            new ShopItem("Enchanted Book (Efficiency V)", "minecraft:enchanted_book", 2000, "§5📖"),
            new ShopItem("Enchanted Book (Sharpness V)", "minecraft:enchanted_book", 2000, "§5📖"),
            new ShopItem("Enchanted Book (Protection IV)", "minecraft:enchanted_book", 1800, "§5📖"),
            new ShopItem("Enchanted Book (Feather Falling IV)", "minecraft:enchanted_book", 1200, "§5📖"),
            new ShopItem("Enchanted Book (Silk Touch)", "minecraft:enchanted_book", 2000, "§5📖"),
            new ShopItem("Enchanted Book (Looting III)", "minecraft:enchanted_book", 2200, "§5📖")
        },
        // Rare
        {
            new ShopItem("Elytra", "minecraft:elytra", 15000, "§7🪽"),
            new ShopItem("Totem of Undying", "minecraft:totem_of_undying", 5000, "§a☀"),
            new ShopItem("Nether Star", "minecraft:nether_star", 25000, "§f★"),
            new ShopItem("Dragon Egg", "minecraft:dragon_egg", 50000, "§5🥚"),
            new ShopItem("Dragon Head", "minecraft:dragon_head", 20000, "§5🐉"),
            new ShopItem("Beacon", "minecraft:beacon", 30000, "§b✦"),
            new ShopItem("Conduit", "minecraft:conduit", 10000, "§3⬤"),
            new ShopItem("Heart of the Sea", "minecraft:heart_of_the_sea", 5000, "§9💙"),
            new ShopItem("Nautilus Shell x4", "minecraft:nautilus_shell", 2000, "§f🐚", 4),
            new ShopItem("Shulker Shell x2", "minecraft:shulker_shell", 3000, "§d◇", 2),
            new ShopItem("Shulker Box", "minecraft:shulker_box", 8000, "§d📦"),
            new ShopItem("End Crystal x4", "minecraft:end_crystal", 4000, "§d✦", 4),
            new ShopItem("Wither Skeleton Skull", "minecraft:wither_skeleton_skull", 10000, "§8💀"),
            new ShopItem("Music Disc (Pigstep)", "minecraft:music_disc_pigstep", 5000, "§d💿"),
            new ShopItem("Disc Fragment x9", "minecraft:disc_fragment_5", 8000, "§6◇", 9),
            new ShopItem("Echo Shard x8", "minecraft:echo_shard", 4000, "§3◇", 8),
            new ShopItem("Recovery Compass", "minecraft:recovery_compass", 10000, "§3🧭")
        }
    };
    
    /**
     * Represents a single shop item
     */
    public static class ShopItem {
        public final String name;
        public final String itemId;
        public final int price;
        public final String icon;
        public final int count;
        
        public ShopItem(String name, String itemId, int price, String icon) {
            this(name, itemId, price, icon, 1);
        }
        
        public ShopItem(String name, String itemId, int price, String icon, int count) {
            this.name = name;
            this.itemId = itemId;
            this.price = price;
            this.icon = icon;
            this.count = count;
        }
    }
    
    /**
     * Get the item names for a category (for display)
     */
    public static String[] getItemNames(int category) {
        if (category < 0 || category >= ITEMS.length) return new String[0];
        String[] names = new String[ITEMS[category].length];
        for (int i = 0; i < ITEMS[category].length; i++) {
            names[i] = ITEMS[category][i].name;
        }
        return names;
    }
    
    /**
     * Get the item icons for a category
     */
    public static String[] getItemIcons(int category) {
        if (category < 0 || category >= ITEMS.length) return new String[0];
        String[] icons = new String[ITEMS[category].length];
        for (int i = 0; i < ITEMS[category].length; i++) {
            icons[i] = ITEMS[category][i].icon;
        }
        return icons;
    }
    
    /**
     * Get the item prices for a category
     */
    public static int[] getItemPrices(int category) {
        if (category < 0 || category >= ITEMS.length) return new int[0];
        int[] prices = new int[ITEMS[category].length];
        for (int i = 0; i < ITEMS[category].length; i++) {
            prices[i] = ITEMS[category][i].price;
        }
        return prices;
    }
    
    /**
     * Get a specific shop item
     */
    public static ShopItem getItem(int category, int index) {
        if (category < 0 || category >= ITEMS.length) return null;
        if (index < 0 || index >= ITEMS[category].length) return null;
        return ITEMS[category][index];
    }
}
