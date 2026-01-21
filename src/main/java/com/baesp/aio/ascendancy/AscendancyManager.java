package com.baesp.aio.ascendancy;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public class AscendancyManager {
    
    // Upgrade Types
    public static final String UPGRADE_VITALITY = "vitality";      // +Health
    public static final String UPGRADE_SWIFTNESS = "swiftness";    // +Speed
    public static final String UPGRADE_REACH = "reach";            // +Reach
    public static final String UPGRADE_HASTE = "haste";            // +Mining speed
    public static final String UPGRADE_FORTUNE = "fortune";        // +Drop rates
    public static final String UPGRADE_MIGHT = "might";            // +Attack damage
    public static final String UPGRADE_RESILIENCE = "resilience";  // +Damage reduction
    public static final String UPGRADE_WISDOM = "wisdom";          // +XP gain
    public static final String UPGRADE_KEEPER = "keeper";          // +Inventory slots
    
    public static final int MAX_UPGRADE_LEVEL = 10;
    
    public static void init() {
        if (!AioMod.CONFIG.ascendancyEnabled) {
            AioMod.LOGGER.info("Ascendancy system disabled in config.");
            return;
        }
        
        // Kill event for Soul XP
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getEntity() instanceof ServerPlayer player) {
                if (entity instanceof Monster) {
                    int xpGain = AioMod.CONFIG.soulXpPerKill;
                    addSoulXp(player, xpGain);
                }
            }
        });
        
        // Block break event for Soul XP (any block + ores bonus)
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                
                // All blocks grant 1 Soul XP
                addSoulXp(serverPlayer, 1);
                
                // Ores grant bonus Soul XP
                if (blockName.contains("ore") || blockName.contains("_ore")) {
                    int bonusXp = AioMod.CONFIG.soulXpPerOreBreak - 1;
                    if (bonusXp > 0) {
                        addSoulXp(serverPlayer, bonusXp);
                    }
                }
            }
        });
        
        AioMod.LOGGER.info("Ascendancy system initialized.");
    }
    
    public static void addSoulXp(ServerPlayer player, long amount) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Apply wisdom bonus
        int wisdomLevel = data.getUpgradeLevel(UPGRADE_WISDOM);
        double multiplier = 1.0 + (wisdomLevel * 0.1); // +10% per level
        amount = (long) (amount * multiplier);
        
        // Apply ascension multiplier
        multiplier = Math.pow(AioMod.CONFIG.ascensionXpMultiplier, data.ascensionCount);
        amount = (long) (amount * multiplier);
        
        data.soulXp += amount;
        
        // Check for level up
        while (data.soulXp >= data.soulXpToNextLevel) {
            data.soulXp -= data.soulXpToNextLevel;
            data.soulLevel++;
            data.soulXpToNextLevel = calculateXpForLevel(data.soulLevel);
            
            player.sendSystemMessage(
                Component.literal("§6✦ §eSoul Level Up! §6Level " + data.soulLevel)
            );
        }
    }
    
    public static long calculateXpForLevel(int level) {
        // Exponential scaling: 100, 150, 225, 338, ...
        return (long) (100 * Math.pow(1.5, level - 1));
    }
    
    public static boolean canPurchaseUpgrade(ServerPlayer player, String upgrade) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        int currentLevel = data.getUpgradeLevel(upgrade);
        
        if (currentLevel >= MAX_UPGRADE_LEVEL) {
            return false;
        }
        
        int cost = getUpgradeCost(upgrade, currentLevel + 1);
        return data.prestigePoints >= cost;
    }
    
    public static int getUpgradeCost(String upgrade, int level) {
        // Cost increases per level: 1, 2, 3, 4, 5...
        return level;
    }
    
    public static boolean purchaseUpgrade(ServerPlayer player, String upgrade) {
        if (!canPurchaseUpgrade(player, upgrade)) {
            return false;
        }
        
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        int currentLevel = data.getUpgradeLevel(upgrade);
        int cost = getUpgradeCost(upgrade, currentLevel + 1);
        
        data.prestigePoints -= cost;
        data.setUpgradeLevel(upgrade, currentLevel + 1);
        
        applyUpgradeEffects(player);
        
        return true;
    }
    
    public static void applyUpgradeEffects(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Vitality - extra hearts
        int vitalityLevel = data.getUpgradeLevel(UPGRADE_VITALITY);
        // Max health handled by attribute modifier
        
        // Swiftness - movement speed
        int swiftnessLevel = data.getUpgradeLevel(UPGRADE_SWIFTNESS);
        // Speed handled by attribute modifier
        
        // Other upgrades are applied in their respective systems
    }
    
    public static void performAscension(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Grant prestige points
        data.prestigePoints += AioMod.CONFIG.prestigePointsPerAscension;
        data.ascensionCount++;
        
        // Reset soul level and XP
        data.soulLevel = 1;
        data.soulXp = 0;
        data.soulXpToNextLevel = calculateXpForLevel(1);
        
        // Clear inventory (hardcore ascension)
        player.getInventory().clearContent();
        
        // Clear ender chest
        player.getEnderChestInventory().clearContent();
        
        // Reset to spawn - get server via the player's level
        ServerLevel overworld = ((ServerLevel) player.level()).getServer().overworld();
        // Use world origin as spawn position
        BlockPos spawnPos = BlockPos.ZERO;
        player.teleportTo(
            overworld,
            spawnPos.getX() + 0.5,
            64.0, // Safe y level
            spawnPos.getZ() + 0.5,
            java.util.Set.of(),
            player.getYRot(),
            player.getXRot(),
            false
        );
        
        player.sendSystemMessage(
            Component.literal("§d✦ §5ASCENSION COMPLETE! §d✦")
        );
        player.sendSystemMessage(
            Component.literal("§7You have gained §e" + AioMod.CONFIG.prestigePointsPerAscension + " Prestige Point(s)§7!")
        );
        player.sendSystemMessage(
            Component.literal("§7Total Ascensions: §b" + data.ascensionCount)
        );
    }
    
    public static double getFortuneBonus(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        int fortuneLevel = data.getUpgradeLevel(UPGRADE_FORTUNE);
        return fortuneLevel * 0.05; // +5% per level
    }
    
    public static double getMightBonus(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        int mightLevel = data.getUpgradeLevel(UPGRADE_MIGHT);
        return mightLevel * 0.05; // +5% per level
    }
    
    public static double getResilienceBonus(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        int resilienceLevel = data.getUpgradeLevel(UPGRADE_RESILIENCE);
        return resilienceLevel * 0.03; // +3% damage reduction per level
    }
    
    // Static accessors for network sync
    public static int getSoulLevel(ServerPlayer player) {
        return PlayerDataManager.getData(player).ascendancy.soulLevel;
    }
    
    public static int getSoulXp(ServerPlayer player) {
        return (int) PlayerDataManager.getData(player).ascendancy.soulXp;
    }
    
    public static int getXpForNextLevel(int level) {
        return (int) calculateXpForLevel(level + 1);
    }
    
    public static int getAscensionCount(ServerPlayer player) {
        return PlayerDataManager.getData(player).ascendancy.ascensionCount;
    }
    
    public static int getPrestigePoints(ServerPlayer player) {
        return PlayerDataManager.getData(player).ascendancy.prestigePoints;
    }
    
    public static int getUpgradeLevel(ServerPlayer player, int upgradeIndex) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        String[] upgrades = {UPGRADE_VITALITY, UPGRADE_SWIFTNESS, UPGRADE_MIGHT, UPGRADE_RESILIENCE,
                             UPGRADE_HASTE, UPGRADE_FORTUNE, UPGRADE_WISDOM, UPGRADE_REACH, UPGRADE_KEEPER};
        if (upgradeIndex >= 0 && upgradeIndex < upgrades.length) {
            return data.getUpgradeLevel(upgrades[upgradeIndex]);
        }
        return 0;
    }
    
    public static void buyUpgrade(ServerPlayer player, int upgradeIndex) {
        String[] upgrades = {UPGRADE_VITALITY, UPGRADE_SWIFTNESS, UPGRADE_MIGHT, UPGRADE_RESILIENCE,
                             UPGRADE_HASTE, UPGRADE_FORTUNE, UPGRADE_WISDOM, UPGRADE_REACH, UPGRADE_KEEPER};
        if (upgradeIndex >= 0 && upgradeIndex < upgrades.length) {
            if (purchaseUpgrade(player, upgrades[upgradeIndex])) {
                player.sendSystemMessage(Component.literal("§aUpgrade purchased!"));
            } else {
                player.sendSystemMessage(Component.literal("§cCannot purchase upgrade!"));
            }
        }
    }
    
    public static void ascend(ServerPlayer player) {
        int minLevel = 10; // Minimum level to ascend
        if (PlayerDataManager.getData(player).ascendancy.soulLevel >= minLevel) {
            performAscension(player);
        } else {
            player.sendSystemMessage(Component.literal("§cYou need Soul Level " + minLevel + " to ascend!"));
        }
    }
}
