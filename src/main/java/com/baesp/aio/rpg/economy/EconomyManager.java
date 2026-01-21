package com.baesp.aio.rpg.economy;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;

public class EconomyManager {
    
    public static void init() {
        if (!AioMod.CONFIG.economyEnabled) {
            AioMod.LOGGER.info("Economy system disabled in config.");
            return;
        }
        
        AioMod.LOGGER.info("Economy system initialized.");
    }
    
    public static long getMoney(ServerPlayer player) {
        return PlayerDataManager.getData(player).economy.money;
    }
    
    public static boolean canAfford(ServerPlayer player, long amount) {
        return PlayerDataManager.getData(player).economy.canAfford(amount);
    }
    
    public static boolean withdraw(ServerPlayer player, long amount) {
        return PlayerDataManager.getData(player).economy.withdraw(amount);
    }
    
    public static void deposit(ServerPlayer player, long amount) {
        PlayerDataManager.getData(player).economy.deposit(amount);
    }
    
    public static void setMoney(ServerPlayer player, long amount) {
        PlayerDataManager.getData(player).economy.money = Math.max(0, amount);
    }
    
    public static String formatMoney(long amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }
    
    public static void giveSmeltingReward(ServerPlayer player) {
        if (AioMod.CONFIG.smeltingRewardCoins > 0) {
            deposit(player, AioMod.CONFIG.smeltingRewardCoins);
        }
    }
}
