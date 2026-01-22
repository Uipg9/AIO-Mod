package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.mixin.MerchantMenuAccessor;
import com.baesp.aio.mixin.VillagerAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;

/**
 * Trade Cycling System
 * 
 * Allows cycling villager trades via button click in the trading UI.
 * Based on the trade cycling mechanics from henkelmax's Trade Cycling mod.
 * 
 * Features:
 * - Button in trading UI to cycle trades automatically
 * - Works only on Novice-level villagers (no trades completed)
 * - No need to manually break/replace job site blocks
 * 
 * How it works:
 * - Clears the villager's current offers
 * - Forces regeneration of new trades
 * - Updates the merchant screen with new offers
 */
public class TradeCyclingManager {
    
    public static void register() {
        AioMod.LOGGER.info("Trade Cycling Manager registered.");
    }
    
    /**
     * Called when player clicks the cycle trades button in the trading UI.
     * This is the main entry point from the network packet handler.
     */
    public static void cycleTrades(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        // Check if player has a merchant menu open
        if (!(player.containerMenu instanceof MerchantMenu container)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Trade Cycling] §7No trading menu open!"
            ));
            return;
        }
        
        // Get the merchant accessor
        if (!(player.containerMenu instanceof MerchantMenuAccessor merchantAccessor)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Trade Cycling] §7Failed to access merchant menu!"
            ));
            return;
        }
        
        Merchant merchant = merchantAccessor.getTrader();
        
        // Check if this is a villager (not a wandering trader)
        if (!(merchant instanceof Villager villager)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Trade Cycling] §7Only works on villagers, not wandering traders!"
            ));
            return;
        }
        
        // Check if villager has XP (has been traded with)
        if (container.getTraderXp() > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Trade Cycling] §7This villager has already been traded with!"
            ));
            return;
        }
        
        // Check if a trade is currently selected/active
        if (merchantAccessor.getTradeContainer().getActiveOffer() != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Trade Cycling] §7Remove items from trade slots first!"
            ));
            return;
        }
        
        // Check if villager has a job site (required for trade cycling)
        if (villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[Trade Cycling] §7Villager needs a job site block to cycle trades!"
            ));
            return;
        }
        
        // CYCLE THE TRADES!
        // Step 1: Clear current offers
        villager.setOffers(null);
        
        // Step 2: Force regeneration of offers by calling getOffers()
        villager.getOffers();
        
        // Step 3: Update special prices for this player (reputation-based discounts, etc.)
        if (villager instanceof VillagerAccessor villagerAccessor) {
            villagerAccessor.invokeUpdateSpecialPrices(player);
        }
        
        // Step 4: Set the trading player again
        villager.setTradingPlayer(player);
        
        // Step 5: Send the new offers to the client
        int villagerLevel = villager.getVillagerData().level();
        player.sendMerchantOffers(
            container.containerId,
            villager.getOffers(),
            villagerLevel,
            villager.getVillagerXp(),
            villager.showProgressBar(),
            villager.canRestock()
        );
        
        // No chat message - the UI updates silently (no spam!)
    }
}
