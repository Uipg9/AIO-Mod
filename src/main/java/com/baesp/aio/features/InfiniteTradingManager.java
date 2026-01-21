package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Infinite Trading System
 * 
 * Prevents villager trades from locking out.
 * Trades reset their "uses" periodically so they're always available.
 * 
 * Features:
 * - Trades never become unavailable due to overuse
 * - Villagers still gain XP from trades normally
 * - Prices still fluctuate based on demand/reputation
 * 
 * Inspired by Serilum's Infinite Trading mod.
 */
public class InfiniteTradingManager {
    
    // How often to reset trade uses (in ticks)
    private static final int RESET_INTERVAL = 200; // Every 10 seconds
    
    private static int tickCounter = 0;
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            
            if (tickCounter >= RESET_INTERVAL) {
                tickCounter = 0;
                
                // Reset trade uses for all villagers in all worlds
                for (ServerLevel level : server.getAllLevels()) {
                    resetVillagerTrades(level);
                }
            }
        });
        
        AioMod.LOGGER.info("Infinite Trading Manager registered.");
    }
    
    private static void resetVillagerTrades(ServerLevel level) {
        // Get all villagers in the level
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Merchant villager) {
                MerchantOffers offers = villager.getOffers();
                
                for (MerchantOffer offer : offers) {
                    // Reset uses to 0 if they've been used
                    if (offer.getUses() > 0) {
                        resetOfferUses(offer);
                    }
                }
            }
        }
    }
    
    private static void resetOfferUses(MerchantOffer offer) {
        // Reset the uses counter using reflection
        try {
            java.lang.reflect.Field usesField = MerchantOffer.class.getDeclaredField("uses");
            usesField.setAccessible(true);
            usesField.setInt(offer, 0);
        } catch (Exception e) {
            // Fallback: try alternative field names (obfuscated)
            try {
                for (java.lang.reflect.Field field : MerchantOffer.class.getDeclaredFields()) {
                    if (field.getType() == int.class) {
                        field.setAccessible(true);
                        int value = field.getInt(offer);
                        // The uses field is typically a small positive number
                        if (value > 0 && value == offer.getUses()) {
                            field.setInt(offer, 0);
                            break;
                        }
                    }
                }
            } catch (Exception e2) {
                // Silently fail - trades will still work normally
            }
        }
    }
}
