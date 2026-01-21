package com.baesp.aio.ascendancy;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

public class AscendancyData {
    // Soul XP system (separate from vanilla XP)
    public long soulXp = 0;
    public int soulLevel = 1;
    public long soulXpToNextLevel = 100;
    
    // Prestige/Ascension
    public int ascensionCount = 0;
    public int prestigePoints = 0;
    
    // Upgrades - key: upgrade name, value: level
    public Map<String, Integer> upgrades = new HashMap<>();
    
    // Constellation (special perk tree)
    public String selectedConstellation = "";
    
    // Achievements
    public Map<String, Boolean> achievements = new HashMap<>();
    
    public void load(CompoundTag tag) {
        soulXp = tag.getLongOr("SoulXp", 0L);
        soulLevel = tag.getIntOr("SoulLevel", 1);
        soulXpToNextLevel = tag.getLongOr("SoulXpToNextLevel", 100L);
        ascensionCount = tag.getIntOr("AscensionCount", 0);
        prestigePoints = tag.getIntOr("PrestigePoints", 0);
        selectedConstellation = tag.getStringOr("Constellation", "");
        
        // Load upgrades
        upgrades.clear();
        CompoundTag upgradesTag = tag.getCompoundOrEmpty("Upgrades");
        for (String key : upgradesTag.keySet()) {
            upgrades.put(key, upgradesTag.getIntOr(key, 0));
        }
        
        // Load achievements
        achievements.clear();
        CompoundTag achievementsTag = tag.getCompoundOrEmpty("Achievements");
        for (String key : achievementsTag.keySet()) {
            achievements.put(key, achievementsTag.getBooleanOr(key, false));
        }
    }
    
    public void save(CompoundTag tag) {
        tag.putLong("SoulXp", soulXp);
        tag.putInt("SoulLevel", soulLevel);
        tag.putLong("SoulXpToNextLevel", soulXpToNextLevel);
        tag.putInt("AscensionCount", ascensionCount);
        tag.putInt("PrestigePoints", prestigePoints);
        tag.putString("Constellation", selectedConstellation);
        
        // Save upgrades
        CompoundTag upgradesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
            upgradesTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Upgrades", upgradesTag);
        
        // Save achievements
        CompoundTag achievementsTag = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : achievements.entrySet()) {
            achievementsTag.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("Achievements", achievementsTag);
    }
    
    public int getUpgradeLevel(String upgrade) {
        return upgrades.getOrDefault(upgrade, 0);
    }
    
    public void setUpgradeLevel(String upgrade, int level) {
        upgrades.put(upgrade, level);
    }
    
    public boolean hasAchievement(String achievement) {
        return achievements.getOrDefault(achievement, false);
    }
    
    public void unlockAchievement(String achievement) {
        achievements.put(achievement, true);
    }
}
