package com.baesp.aio.ascendancy;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import com.baesp.aio.features.StarterKitManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
    
    public static final int REQUIRED_SOUL_LEVEL_FOR_ASCENSION = 5;
    public static final int MAX_UPGRADE_LEVEL = 10;
    
    public static void init() {
        if (!AioMod.CONFIG.ascendancyEnabled) {
            AioMod.LOGGER.info("Ascendancy system disabled in config.");
            return;
        }
        
        // Kill event for Soul XP
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getEntity() instanceof ServerPlayer player) {
                int xpGain = 0;
                long moneyGain = 0;
                
                if (entity instanceof Monster) {
                    // Hostile mobs
                    xpGain = AioMod.CONFIG.soulXpPerKill;
                    moneyGain = 5; // $5 per hostile mob
                } else if (entity instanceof net.minecraft.world.entity.animal.Animal ||
                           entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) {
                    // Passive mobs (cows, pigs, sheep, chickens, bats, etc.)
                    xpGain = Math.max(1, AioMod.CONFIG.soulXpPerKill / 2); // Half XP for passive
                    moneyGain = 2; // $2 per passive mob
                }
                
                if (xpGain > 0) {
                    addSoulXp(player, xpGain);
                }
                if (moneyGain > 0) {
                    com.baesp.aio.rpg.economy.EconomyManager.deposit(player, moneyGain);
                    player.sendSystemMessage(
                        Component.literal("§6+$" + moneyGain + " §7from kill")
                    );
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
        
        // Respawn at ascension location
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) { // Player died and respawned
                teleportToAscensionSpawn(newPlayer);
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
    
    // Attribute modifier UUIDs for consistent tracking
    private static final java.util.UUID VITALITY_UUID = java.util.UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final java.util.UUID SWIFTNESS_UUID = java.util.UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final java.util.UUID MIGHT_UUID = java.util.UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    
    public static void applyUpgradeEffects(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Vitality - extra hearts (+2 HP per level = +1 heart per level)
        int vitalityLevel = data.getUpgradeLevel(UPGRADE_VITALITY);
        applyHealthBonus(player, vitalityLevel);
        
        // Swiftness - movement speed (+5% per level)
        int swiftnessLevel = data.getUpgradeLevel(UPGRADE_SWIFTNESS);
        applySpeedBonus(player, swiftnessLevel);
        
        // Might - attack damage (+0.5 per level)
        int mightLevel = data.getUpgradeLevel(UPGRADE_MIGHT);
        applyDamageBonus(player, mightLevel);
        
        // Other upgrades are applied in their respective systems
        PlayerDataManager.savePlayer(player);
    }
    
    private static void applyHealthBonus(ServerPlayer player, int level) {
        var healthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (healthAttr == null) return;
        
        // Remove old modifier if exists
        var modifierId = net.minecraft.resources.Identifier.fromNamespaceAndPath("aio", "vitality_bonus");
        healthAttr.removeModifier(modifierId);
        
        // Add new modifier (+2 HP per level = +1 heart per level)
        if (level > 0) {
            double bonus = level * 2.0; // Each level = +2 HP = +1 heart
            var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                modifierId,
                bonus,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );
            healthAttr.addPermanentModifier(modifier);
            
            // Heal player to new max if needed
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(Math.min(player.getHealth() + (float)bonus, player.getMaxHealth()));
            }
        }
    }
    
    private static void applySpeedBonus(ServerPlayer player, int level) {
        var speedAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;
        
        // Remove old modifier
        var modifierId = net.minecraft.resources.Identifier.fromNamespaceAndPath("aio", "swiftness_bonus");
        speedAttr.removeModifier(modifierId);
        
        // Add new modifier (+5% speed per level)
        if (level > 0) {
            double bonus = level * 0.05; // 5% per level
            var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                modifierId,
                bonus,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            speedAttr.addPermanentModifier(modifier);
        }
    }
    
    private static void applyDamageBonus(ServerPlayer player, int level) {
        var damageAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (damageAttr == null) return;
        
        // Remove old modifier
        var modifierId = net.minecraft.resources.Identifier.fromNamespaceAndPath("aio", "might_bonus");
        damageAttr.removeModifier(modifierId);
        
        // Add new modifier (+0.5 damage per level)
        if (level > 0) {
            double bonus = level * 0.5;
            var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                modifierId,
                bonus,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
            );
            damageAttr.addPermanentModifier(modifier);
        }
    }
    
    public static boolean canAscend(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        return data.soulLevel >= REQUIRED_SOUL_LEVEL_FOR_ASCENSION;
    }
    
    public static void performAscension(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Grant MORE prestige points (3 instead of 1)
        int pointsGained = 3;
        data.prestigePoints += pointsGained;
        data.ascensionCount++;
        
        // Reset soul level and XP
        data.soulLevel = 1;
        data.soulXp = 0;
        data.soulXpToNextLevel = calculateXpForLevel(1);
        
        // Clear inventory (hardcore ascension)
        player.getInventory().clearContent();
        
        // Clear ender chest
        player.getEnderChestInventory().clearContent();
        
        // DON'T give starter kit here - the player joining event will handle it
        // This was causing item duplication!
        // StarterKitManager.giveStarterKit(player);
        
        // Find a DISTANT village for ascension spawn
        ServerLevel overworld = ((ServerLevel) player.level()).getServer().overworld();
        BlockPos villagePos = com.baesp.aio.villagespawn.VillageSpawnManager.findDistantVillage(overworld);
        
        double spawnX, spawnY, spawnZ;
        if (villagePos != null) {
            spawnX = villagePos.getX() + 0.5;
            spawnZ = villagePos.getZ() + 0.5;
            // Spawn HIGH in the sky (y=200) so they fall down with effects
            spawnY = 200.0;
        } else {
            // Fallback: random distant location in sky
            java.util.Random rand = new java.util.Random();
            int distance = 5000 + rand.nextInt(3000);  // 5000-8000 blocks away
            double angle = rand.nextDouble() * 2 * Math.PI;
            spawnX = Math.cos(angle) * distance;
            spawnZ = Math.sin(angle) * distance;
            spawnY = 200.0;
        }
        
        // Set time to day and clear weather for fresh start
        overworld.setDayTime(0);
        overworld.setWeatherParameters(6000, 0, false, false); // Clear weather for 5 minutes
        
        // Give protective effects for the fall
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.SLOW_FALLING, 
            600, // 30 seconds
            0, 
            false, 
            true,
            true
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.RESISTANCE, 
            600, // 30 seconds
            4,   // Level 5 = immunity
            false, 
            true,
            true
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.BLINDNESS, 
            300, // 15 seconds for dramatic effect
            0, 
            false, 
            false,
            false
        ));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.GLOWING, 
            200, // 10 seconds
            0, 
            false, 
            true,
            true
        ));
        // Saturation for food
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.SATURATION, 
            600, // 30 seconds
            1,   // Level 2
            false, 
            false,
            true
        ));
        // Regeneration for health
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.REGENERATION, 
            600, // 30 seconds
            1,   // Level 2
            false, 
            true,
            true
        ));
        
        player.teleportTo(
            overworld,
            spawnX,
            spawnY,
            spawnZ,
            java.util.Set.of(),
            player.getYRot(),
            player.getXRot(),
            false
        );
        
        // Save ascension spawn point IMMEDIATELY (at ground level)
        final double groundY = overworld.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 
            new BlockPos((int)spawnX, 64, (int)spawnZ)
        ).getY() + 1.0;
        
        data.ascensionSpawnX = spawnX;
        data.ascensionSpawnY = groundY; // Ground level, not sky
        data.ascensionSpawnZ = spawnZ;
        data.ascensionSpawnDimension = overworld.dimension().toString();
        PlayerDataManager.savePlayer(player);
        
        // CRITICAL: Update VillageSpawnManager with this player's new village spawn!
        // This ensures they respawn at their new village after death
        BlockPos newVillageSpawn = new BlockPos((int)spawnX, (int)groundY, (int)spawnZ);
        com.baesp.aio.villagespawn.VillageSpawnManager.setPlayerVillageSpawn(player.getUUID(), newVillageSpawn);
        
        // Give starter kit items directly (not through StarterKitManager to avoid tracking issues)
        giveAscensionKit(player);
        
        player.sendSystemMessage(
            Component.literal("§d✦ §5ASCENSION COMPLETE! §d✦")
        );
        player.sendSystemMessage(
            Component.literal("§7You have gained §e" + pointsGained + " Prestige Point(s)§7!")
        );
        player.sendSystemMessage(
            Component.literal("§7Total Ascensions: §b" + data.ascensionCount)
        );
        player.sendSystemMessage(
            Component.literal("§7You are descending to a §enew distant land§7...")
        );
    }
    
    /**
     * Give basic kit on ascension without triggering the starter kit tracking
     */
    private static void giveAscensionKit(ServerPlayer player) {
        // Basic tools
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_AXE));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SHOVEL));
        
        // Armor
        player.getInventory().setItem(36, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_BOOTS));
        player.getInventory().setItem(37, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS));
        player.getInventory().setItem(38, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE));
        player.getInventory().setItem(39, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        
        // Food
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 16));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 8));
        
        // Utility
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH, 32));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE));
    }
    
    private static void teleportToAscensionSpawn(ServerPlayer player) {
        AscendancyData data = PlayerDataManager.getData(player).ascendancy;
        
        // Check if ascension spawn is set
        if (Double.isNaN(data.ascensionSpawnX) || data.ascensionSpawnDimension == null) {
            return; // No ascension spawn saved yet
        }
        
        // Get the dimension
        var server = ((ServerLevel) player.level()).getServer();
        ServerLevel targetLevel = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().toString().equals(data.ascensionSpawnDimension)) {
                targetLevel = level;
                break;
            }
        }
        
        if (targetLevel == null) {
            AioMod.LOGGER.warn("Could not find dimension: " + data.ascensionSpawnDimension);
            return;
        }
        
        // Force load the chunk and find a SAFE Y coordinate at the spawn location
        int spawnX = (int) data.ascensionSpawnX;
        int spawnZ = (int) data.ascensionSpawnZ;
        targetLevel.getChunk(spawnX >> 4, spawnZ >> 4);
        
        // Find safe Y by scanning down from top to find actual surface
        double safeY = findSafeY(targetLevel, spawnX, spawnZ);
        
        // Update the saved spawn Y to prevent future cave spawns
        if (Math.abs(safeY - data.ascensionSpawnY) > 5) {
            data.ascensionSpawnY = safeY;
            PlayerDataManager.savePlayer(player);
            AioMod.LOGGER.info("Updated ascension spawn Y from cave to surface: " + safeY);
        }
        
        // Teleport to safe surface position
        player.teleportTo(
            targetLevel,
            data.ascensionSpawnX,
            safeY,
            data.ascensionSpawnZ,
            java.util.Set.of(),
            player.getYRot(),
            player.getXRot(),
            false
        );
        
        player.sendSystemMessage(
            Component.literal("§d✦ §7Respawned at ascension location")
        );
    }
    
    /**
     * Finds a safe Y coordinate by scanning DOWN from max height to find the actual surface.
     * This prevents spawning in caves.
     */
    private static double findSafeY(ServerLevel world, int x, int z) {
        // Force chunk to be fully loaded/generated
        world.getChunk(x >> 4, z >> 4);
        
        // Scan DOWN from top to find the first solid block (the actual surface)
        for (int y = 319; y > 50; y--) {
            net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(x, y, z);
            net.minecraft.core.BlockPos abovePos = new net.minecraft.core.BlockPos(x, y + 1, z);
            net.minecraft.core.BlockPos above2Pos = new net.minecraft.core.BlockPos(x, y + 2, z);
            
            var blockState = world.getBlockState(checkPos);
            var aboveState = world.getBlockState(abovePos);
            var above2State = world.getBlockState(above2Pos);
            
            // Skip air/water - we're looking for ground
            if (blockState.isAir() || !blockState.getFluidState().isEmpty()) {
                continue;
            }
            
            // Skip leaves and other non-solid blocks
            if (!blockState.isSolid()) {
                continue;
            }
            
            // Check if we found solid ground with 2 air blocks above (standing space)
            boolean abovePassable = aboveState.isAir() || (!aboveState.isSolid() && aboveState.getFluidState().isEmpty());
            boolean above2Passable = above2State.isAir() || (!above2State.isSolid() && above2State.getFluidState().isEmpty());
            
            if (abovePassable && above2Passable) {
                return y + 1.0; // Stand on top of the solid block
            }
        }
        
        // Fallback to heightmap
        net.minecraft.core.BlockPos heightmapPos = world.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new net.minecraft.core.BlockPos(x, 64, z)
        );
        return heightmapPos.getY() + 1.0;
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
        if (PlayerDataManager.getData(player).ascendancy.soulLevel >= REQUIRED_SOUL_LEVEL_FOR_ASCENSION) {
            performAscension(player);
        } else {
            player.sendSystemMessage(Component.literal("§cYou need Soul Level " + REQUIRED_SOUL_LEVEL_FOR_ASCENSION + " to ascend!"));
        }
    }
}
