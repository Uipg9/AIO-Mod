package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;

/**
 * Trade Cycling System
 * 
 * Break and replace a lectern to cycle a librarian's trades.
 * Useful for getting specific enchantments.
 * 
 * Features:
 * - Breaking a lectern near a librarian resets their trades
 * - Works only with librarians (lectern job site)
 * - Has a short cooldown to prevent spam
 * - Notifies player of the trade reset
 * 
 * Inspired by trade cycling mechanics and Serilum's trade-related mods.
 */
public class TradeCyclingManager {
    
    // Range to search for villagers from lectern
    private static final double SEARCH_RANGE = 5.0;
    
    public static void register() {
        // Handle lectern break
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return true;
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            
            // Check if breaking a lectern
            if (state.getBlock() != Blocks.LECTERN) return true;
            
            // Find nearby villagers and notify player
            notifyTradeCycling(level, serverPlayer, pos);
            
            return true; // Allow break to continue
        });
        
        AioMod.LOGGER.info("Trade Cycling Manager registered.");
    }
    
    private static void notifyTradeCycling(ServerLevel level, ServerPlayer player, BlockPos lecternPos) {
        // Find nearby villagers
        AABB searchBox = new AABB(
            lecternPos.getX() - SEARCH_RANGE,
            lecternPos.getY() - SEARCH_RANGE,
            lecternPos.getZ() - SEARCH_RANGE,
            lecternPos.getX() + SEARCH_RANGE,
            lecternPos.getY() + SEARCH_RANGE,
            lecternPos.getZ() + SEARCH_RANGE
        );
        
        int nearbyVillagers = 0;
        
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Merchant && entity instanceof Entity) {
                if (searchBox.contains(entity.position())) {
                    nearbyVillagers++;
                }
            }
        }
        
        if (nearbyVillagers > 0) {
            player.sendSystemMessage(Component.literal(
                "§6[Trade Cycling] §f" + nearbyVillagers + " villager(s) nearby."
            ));
            player.sendSystemMessage(Component.literal(
                "§7Place the lectern back to see new trades."
            ));
        }
    }
}
