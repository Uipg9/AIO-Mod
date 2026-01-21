package com.baesp.aio.rpg;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

public class SkillsData {
    // Skills (from vanilla-plus-rpg)
    public static final String SKILL_FARMING = "farming";
    public static final String SKILL_COMBAT = "combat";
    public static final String SKILL_DEFENSE = "defense";
    public static final String SKILL_SMITHING = "smithing";
    public static final String SKILL_WOODCUTTING = "woodcutting";
    public static final String SKILL_MINING = "mining";
    
    // Skill levels (1-10)
    public Map<String, Integer> skillLevels = new HashMap<>();
    
    // Skill XP
    public Map<String, Integer> skillXp = new HashMap<>();
    
    public SkillsData() {
        // Initialize all skills at level 1
        skillLevels.put(SKILL_FARMING, 1);
        skillLevels.put(SKILL_COMBAT, 1);
        skillLevels.put(SKILL_DEFENSE, 1);
        skillLevels.put(SKILL_SMITHING, 1);
        skillLevels.put(SKILL_WOODCUTTING, 1);
        skillLevels.put(SKILL_MINING, 1);
        
        skillXp.put(SKILL_FARMING, 0);
        skillXp.put(SKILL_COMBAT, 0);
        skillXp.put(SKILL_DEFENSE, 0);
        skillXp.put(SKILL_SMITHING, 0);
        skillXp.put(SKILL_WOODCUTTING, 0);
        skillXp.put(SKILL_MINING, 0);
    }
    
    public void load(CompoundTag tag) {
        // Load skill levels
        CompoundTag levelsTag = tag.getCompoundOrEmpty("Levels");
        for (String skill : new String[]{SKILL_FARMING, SKILL_COMBAT, SKILL_DEFENSE, SKILL_SMITHING, SKILL_WOODCUTTING, SKILL_MINING}) {
            skillLevels.put(skill, levelsTag.getIntOr(skill, 1));
        }
        
        // Load skill XP
        CompoundTag xpTag = tag.getCompoundOrEmpty("Xp");
        for (String skill : new String[]{SKILL_FARMING, SKILL_COMBAT, SKILL_DEFENSE, SKILL_SMITHING, SKILL_WOODCUTTING, SKILL_MINING}) {
            skillXp.put(skill, xpTag.getIntOr(skill, 0));
        }
    }
    
    public void save(CompoundTag tag) {
        // Save skill levels
        CompoundTag levelsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            levelsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Levels", levelsTag);
        
        // Save skill XP
        CompoundTag xpTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skillXp.entrySet()) {
            xpTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Xp", xpTag);
    }
    
    public int getSkillLevel(String skill) {
        return skillLevels.getOrDefault(skill, 1);
    }
    
    public int getSkillXp(String skill) {
        return skillXp.getOrDefault(skill, 0);
    }
    
    public void setSkillLevel(String skill, int level) {
        skillLevels.put(skill, Math.min(level, 10));
    }
    
    public void setSkillXp(String skill, int xp) {
        skillXp.put(skill, xp);
    }
    
    public int getXpForLevel(int level) {
        // XP required: 100, 200, 300, 400, 500, 600, 700, 800, 900, MAX
        return level * 100;
    }
    
    public double getSkillBonus(String skill) {
        int level = getSkillLevel(skill);
        return (level - 1) * 0.05; // 0% at level 1, +5% per level after
    }
}
