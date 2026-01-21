package com.baesp.aio.rpg;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.CropBlock;

public class SkillsManager {
    
    public static void init() {
        if (!AioMod.CONFIG.skillsEnabled) {
            AioMod.LOGGER.info("Skills system disabled in config.");
            return;
        }
        
        // Combat XP from killing mobs
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getEntity() instanceof ServerPlayer player) {
                if (entity instanceof Monster) {
                    addSkillXp(player, SkillsData.SKILL_COMBAT, AioMod.CONFIG.xpPerSkillAction);
                }
            }
        });
        
        // Block break events for Mining, Woodcutting, Farming
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            
            String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            
            // Mining XP
            if (blockName.contains("ore") || blockName.contains("stone") || 
                blockName.contains("deepslate") || blockName.contains("granite") ||
                blockName.contains("diorite") || blockName.contains("andesite")) {
                addSkillXp(serverPlayer, SkillsData.SKILL_MINING, AioMod.CONFIG.xpPerSkillAction);
            }
            
            // Woodcutting XP
            if (blockName.contains("log") || blockName.contains("wood") || 
                blockName.contains("stem") || blockName.contains("hyphae")) {
                addSkillXp(serverPlayer, SkillsData.SKILL_WOODCUTTING, AioMod.CONFIG.xpPerSkillAction);
            }
            
            // Farming XP
            if (state.getBlock() instanceof CropBlock || 
                blockName.contains("wheat") || blockName.contains("carrot") ||
                blockName.contains("potato") || blockName.contains("beetroot") ||
                blockName.contains("melon") || blockName.contains("pumpkin")) {
                addSkillXp(serverPlayer, SkillsData.SKILL_FARMING, AioMod.CONFIG.xpPerSkillAction);
            }
        });
        
        AioMod.LOGGER.info("Skills system initialized.");
    }
    
    public static void addSkillXp(ServerPlayer player, String skill, int amount) {
        SkillsData skills = PlayerDataManager.getData(player).skills;
        
        int currentLevel = skills.getSkillLevel(skill);
        if (currentLevel >= AioMod.CONFIG.maxSkillLevel) {
            return; // Max level reached
        }
        
        int currentXp = skills.getSkillXp(skill);
        int newXp = currentXp + amount;
        int xpForNextLevel = skills.getXpForLevel(currentLevel);
        
        // Check for level up
        while (newXp >= xpForNextLevel && currentLevel < AioMod.CONFIG.maxSkillLevel) {
            newXp -= xpForNextLevel;
            currentLevel++;
            xpForNextLevel = skills.getXpForLevel(currentLevel);
            
            // Level up notification (NOT action bar - user requested no action bar spam)
            player.sendSystemMessage(
                Component.literal("§a⬆ " + formatSkillName(skill) + " leveled up to §e" + currentLevel + "§a!")
            );
        }
        
        skills.setSkillLevel(skill, currentLevel);
        skills.setSkillXp(skill, newXp);
    }
    
    public static String formatSkillName(String skill) {
        return switch (skill) {
            case SkillsData.SKILL_FARMING -> "Farming";
            case SkillsData.SKILL_COMBAT -> "Combat";
            case SkillsData.SKILL_DEFENSE -> "Defense";
            case SkillsData.SKILL_SMITHING -> "Smithing";
            case SkillsData.SKILL_WOODCUTTING -> "Woodcutting";
            case SkillsData.SKILL_MINING -> "Mining";
            default -> skill;
        };
    }
    
    public static double getCombatDamageBonus(ServerPlayer player) {
        return PlayerDataManager.getData(player).skills.getSkillBonus(SkillsData.SKILL_COMBAT);
    }
    
    public static double getDefenseBonus(ServerPlayer player) {
        return PlayerDataManager.getData(player).skills.getSkillBonus(SkillsData.SKILL_DEFENSE);
    }
    
    public static double getMiningSpeedBonus(ServerPlayer player) {
        return PlayerDataManager.getData(player).skills.getSkillBonus(SkillsData.SKILL_MINING);
    }
    
    public static double getWoodcuttingSpeedBonus(ServerPlayer player) {
        return PlayerDataManager.getData(player).skills.getSkillBonus(SkillsData.SKILL_WOODCUTTING);
    }
    
    public static double getFarmingBonus(ServerPlayer player) {
        return PlayerDataManager.getData(player).skills.getSkillBonus(SkillsData.SKILL_FARMING);
    }
    
    // Static accessors for network sync by index
    private static final String[] SKILL_ORDER = {
        SkillsData.SKILL_FARMING, SkillsData.SKILL_COMBAT, SkillsData.SKILL_DEFENSE,
        SkillsData.SKILL_SMITHING, SkillsData.SKILL_WOODCUTTING, SkillsData.SKILL_MINING
    };
    
    public static int getSkillLevel(ServerPlayer player, int index) {
        if (index >= 0 && index < SKILL_ORDER.length) {
            return PlayerDataManager.getData(player).skills.getSkillLevel(SKILL_ORDER[index]);
        }
        return 0;
    }
    
    public static int getSkillXp(ServerPlayer player, int index) {
        if (index >= 0 && index < SKILL_ORDER.length) {
            return PlayerDataManager.getData(player).skills.getSkillXp(SKILL_ORDER[index]);
        }
        return 0;
    }
}
