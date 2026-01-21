package com.baesp.aio.client.gui;

import com.baesp.aio.network.AioNetwork;

/**
 * Client-side cache of player data for rendering
 * Updated via network packets from server
 */
public class ClientDataCache {
    // Singleton instance
    private static final ClientDataCache INSTANCE = new ClientDataCache();
    
    // Ascendancy data
    public int soulLevel = 1;
    public int soulXp = 0;
    public int soulXpToNextLevel = 100;
    public int ascensionCount = 0;
    public int prestigePoints = 0;
    public int[] upgradeLevels = new int[9]; // vitality, swiftness, might, resilience, haste, fortune, wisdom, reach, keeper
    
    // Skills data
    public int[] skillLevels = new int[6]; // farming, combat, defense, smithing, woodcutting, mining
    public int[] skillXp = new int[6];
    
    // Economy data
    public long money = 0;
    
    // Squat grow
    public boolean squatGrowEnabled = false;
    
    public static ClientDataCache get() {
        return INSTANCE;
    }
    
    /**
     * Update all cache data from the server sync packet
     */
    public void updateFromPacket(AioNetwork.SyncDataPacket packet) {
        this.soulLevel = packet.soulLevel();
        this.soulXp = packet.soulXp();
        this.soulXpToNextLevel = packet.soulXpToNextLevel();
        this.ascensionCount = packet.ascensionCount();
        this.prestigePoints = packet.prestigePoints();
        
        int[] skillLvls = packet.skillLevels();
        int[] skillXps = packet.skillXp();
        for (int i = 0; i < Math.min(skillLvls.length, this.skillLevels.length); i++) {
            this.skillLevels[i] = skillLvls[i];
            this.skillXp[i] = skillXps[i];
        }
        
        this.money = packet.money();
        
        int[] upgrades = packet.upgradeLevels();
        for (int i = 0; i < Math.min(upgrades.length, this.upgradeLevels.length); i++) {
            this.upgradeLevels[i] = upgrades[i];
        }
        
        this.squatGrowEnabled = packet.squatGrowEnabled();
    }
    
    public void updateFromAscendancy(int soulLevel, int soulXp, int soulXpNext, int ascensions, int prestige, int[] upgrades) {
        this.soulLevel = soulLevel;
        this.soulXp = soulXp;
        this.soulXpToNextLevel = soulXpNext;
        this.ascensionCount = ascensions;
        this.prestigePoints = prestige;
        if (upgrades != null && upgrades.length <= this.upgradeLevels.length) {
            System.arraycopy(upgrades, 0, this.upgradeLevels, 0, upgrades.length);
        }
    }
    
    public void updateFromSkills(int[] levels, int[] xp) {
        if (levels != null && levels.length <= this.skillLevels.length) {
            System.arraycopy(levels, 0, this.skillLevels, 0, levels.length);
        }
        if (xp != null && xp.length <= this.skillXp.length) {
            System.arraycopy(xp, 0, this.skillXp, 0, xp.length);
        }
    }
    
    public void updateMoney(long amount) {
        this.money = amount;
    }
    
    public void updateSquatGrow(boolean enabled) {
        this.squatGrowEnabled = enabled;
    }
    
    // Skill helpers
    public int getSkillLevel(int index) {
        if (index >= 0 && index < skillLevels.length) {
            return skillLevels[index];
        }
        return 0;
    }
    
    public int getSkillXp(int index) {
        if (index >= 0 && index < skillXp.length) {
            return skillXp[index];
        }
        return 0;
    }
    
    public int getXpForLevel(int level) {
        return level * 100; // Simple formula
    }
    
    // Upgrade helpers
    public int getUpgradeLevel(int index) {
        if (index >= 0 && index < upgradeLevels.length) {
            return upgradeLevels[index];
        }
        return 0;
    }
    
    public String formatMoney() {
        return formatMoney(this.money);
    }
    
    public String formatMoney(long amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }
}
