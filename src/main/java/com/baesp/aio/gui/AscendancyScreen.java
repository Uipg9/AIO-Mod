package com.baesp.aio.gui;

import com.baesp.aio.ascendancy.AscendancyData;
import com.baesp.aio.ascendancy.AscendancyManager;
import com.baesp.aio.data.PlayerDataManager;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class AscendancyScreen extends SimpleGui {
    private final ServerPlayer player;
    
    public AscendancyScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        
        setTitle(Component.literal("§5✦ Ascendancy ✦"));
        setupGui();
    }
    
    private void setupGui() {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Fill background with gray glass
        for (int i = 0; i < 54; i++) {
            setSlot(i, new GuiElementBuilder()
                .setItem(Items.GRAY_STAINED_GLASS_PANE)
                .setName(Component.literal(""))
                .build()
            );
        }
        
        // Purple accent corners
        for (int slot : new int[]{0, 8, 45, 53}) {
            setSlot(slot, new GuiElementBuilder()
                .setItem(Items.PURPLE_STAINED_GLASS_PANE)
                .setName(Component.literal(""))
                .build()
            );
        }
        
        // Player info (center top)
        setSlot(4, new GuiElementBuilder()
            .setItem(Items.PLAYER_HEAD)
            .setSkullOwner(player.getGameProfile(), ((net.minecraft.server.level.ServerLevel) player.level()).getServer())
            .setName(Component.literal("§d" + player.getName().getString()))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Soul Level: §e" + data.soulLevel))
            .addLoreLine(Component.literal("§7Soul XP: §b" + data.soulXp + "§7/§b" + data.soulXpToNextLevel))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Ascensions: §5" + data.ascensionCount))
            .addLoreLine(Component.literal("§7Prestige Points: §e" + data.prestigePoints))
            .build()
        );
        
        // Upgrades row 1
        int[] upgradeSlots = {19, 20, 21, 22, 23, 24, 25};
        String[] upgrades = {
            AscendancyManager.UPGRADE_VITALITY,
            AscendancyManager.UPGRADE_SWIFTNESS,
            AscendancyManager.UPGRADE_MIGHT,
            AscendancyManager.UPGRADE_RESILIENCE,
            AscendancyManager.UPGRADE_HASTE,
            AscendancyManager.UPGRADE_FORTUNE,
            AscendancyManager.UPGRADE_WISDOM
        };
        
        for (int i = 0; i < upgrades.length && i < upgradeSlots.length; i++) {
            final String upgrade = upgrades[i];
            int level = data.getUpgradeLevel(upgrade);
            int cost = AscendancyManager.getUpgradeCost(upgrade, level + 1);
            boolean canAfford = data.prestigePoints >= cost && level < AscendancyManager.MAX_UPGRADE_LEVEL;
            
            setSlot(upgradeSlots[i], new GuiElementBuilder()
                .setItem(getUpgradeItem(upgrade))
                .setName(Component.literal((canAfford ? "§a" : "§c") + formatUpgradeName(upgrade)))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("§7Level: §e" + level + "§7/§e" + AscendancyManager.MAX_UPGRADE_LEVEL))
                .addLoreLine(Component.literal("§7Effect: §b" + getUpgradeDescription(upgrade, level)))
                .addLoreLine(Component.literal(""))
                .addLoreLine(level >= AscendancyManager.MAX_UPGRADE_LEVEL 
                    ? Component.literal("§6MAX LEVEL")
                    : Component.literal("§7Cost: §e" + cost + " Prestige Points"))
                .addLoreLine(canAfford ? Component.literal("§aClick to upgrade!") : Component.literal(""))
                .setCallback((index, type, action) -> {
                    if (AscendancyManager.purchaseUpgrade(player, upgrade)) {
                        player.sendSystemMessage(Component.literal("§a✓ Upgraded " + formatUpgradeName(upgrade) + "!"));
                        setupGui(); // Refresh
                    }
                })
                .build()
            );
        }
        
        // Ascension button (bottom center)
        setSlot(49, new GuiElementBuilder()
            .setItem(Items.NETHER_STAR)
            .setName(Component.literal("§5⚜ ASCEND ⚜"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Reset your progress to gain"))
            .addLoreLine(Component.literal("§e" + AscendancyManager.getUpgradeCost("", 1) + " Prestige Points"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§c⚠ This will clear your inventory!"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§7Use §e/ascension confirm §7to proceed"))
            .glow()
            .build()
        );
        
        // Close button
        setSlot(45, new GuiElementBuilder()
            .setItem(Items.BARRIER)
            .setName(Component.literal("§cClose"))
            .setCallback((index, type, action) -> close())
            .build()
        );
    }
    
    private net.minecraft.world.item.Item getUpgradeItem(String upgrade) {
        return switch (upgrade) {
            case AscendancyManager.UPGRADE_VITALITY -> Items.RED_DYE;
            case AscendancyManager.UPGRADE_SWIFTNESS -> Items.FEATHER;
            case AscendancyManager.UPGRADE_MIGHT -> Items.IRON_SWORD;
            case AscendancyManager.UPGRADE_RESILIENCE -> Items.IRON_CHESTPLATE;
            case AscendancyManager.UPGRADE_HASTE -> Items.GOLDEN_PICKAXE;
            case AscendancyManager.UPGRADE_FORTUNE -> Items.EMERALD;
            case AscendancyManager.UPGRADE_WISDOM -> Items.EXPERIENCE_BOTTLE;
            case AscendancyManager.UPGRADE_REACH -> Items.ENDER_PEARL;
            case AscendancyManager.UPGRADE_KEEPER -> Items.CHEST;
            default -> Items.PAPER;
        };
    }
    
    private String formatUpgradeName(String upgrade) {
        return switch (upgrade) {
            case AscendancyManager.UPGRADE_VITALITY -> "Vitality";
            case AscendancyManager.UPGRADE_SWIFTNESS -> "Swiftness";
            case AscendancyManager.UPGRADE_MIGHT -> "Might";
            case AscendancyManager.UPGRADE_RESILIENCE -> "Resilience";
            case AscendancyManager.UPGRADE_HASTE -> "Haste";
            case AscendancyManager.UPGRADE_FORTUNE -> "Fortune";
            case AscendancyManager.UPGRADE_WISDOM -> "Wisdom";
            case AscendancyManager.UPGRADE_REACH -> "Reach";
            case AscendancyManager.UPGRADE_KEEPER -> "Keeper";
            default -> upgrade;
        };
    }
    
    private String getUpgradeDescription(String upgrade, int level) {
        return switch (upgrade) {
            case AscendancyManager.UPGRADE_VITALITY -> "+" + (level * 2) + " Max Health";
            case AscendancyManager.UPGRADE_SWIFTNESS -> "+" + (level * 5) + "% Speed";
            case AscendancyManager.UPGRADE_MIGHT -> "+" + (level * 5) + "% Attack Damage";
            case AscendancyManager.UPGRADE_RESILIENCE -> "+" + (level * 3) + "% Damage Reduction";
            case AscendancyManager.UPGRADE_HASTE -> "+" + (level * 5) + "% Mining Speed";
            case AscendancyManager.UPGRADE_FORTUNE -> "+" + (level * 5) + "% Drop Rates";
            case AscendancyManager.UPGRADE_WISDOM -> "+" + (level * 10) + "% XP Gain";
            case AscendancyManager.UPGRADE_REACH -> "+" + (level * 0.5) + " Block Reach";
            case AscendancyManager.UPGRADE_KEEPER -> "+" + (level) + " Inventory Rows";
            default -> "Unknown";
        };
    }
    
    public static void open(ServerPlayer player) {
        new AscendancyScreen(player).open();
    }
}
