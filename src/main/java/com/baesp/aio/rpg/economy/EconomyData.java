package com.baesp.aio.rpg.economy;

import com.baesp.aio.AioMod;
import net.minecraft.nbt.CompoundTag;

public class EconomyData {
    public long money = AioMod.CONFIG != null ? AioMod.CONFIG.startingMoney : 100;
    
    public void load(CompoundTag tag) {
        money = tag.getLongOr("Money", AioMod.CONFIG != null ? AioMod.CONFIG.startingMoney : 100);
    }
    
    public void save(CompoundTag tag) {
        tag.putLong("Money", money);
    }
    
    public boolean canAfford(long amount) {
        return money >= amount;
    }
    
    public boolean withdraw(long amount) {
        if (canAfford(amount)) {
            money -= amount;
            return true;
        }
        return false;
    }
    
    public void deposit(long amount) {
        money += amount;
    }
}
