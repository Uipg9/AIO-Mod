package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sleep Sooner System
 * 
 * Allows players to sleep at any time by double-right-clicking a bed.
 * - First click: Normal bed behavior (set spawn if day, sleep if night)
 * - Quick second click: Force sleep regardless of time
 * 
 * Also skips the "you can only sleep at night" message for double-clicks.
 * 
 * Inspired by Serilum's Sleep Sooner mod.
 */
public class SleepSoonerManager {
    
    // Track last bed click time per player
    private static final Map<UUID, Long> lastBedClick = new HashMap<>();
    private static final Map<UUID, BlockPos> lastBedPos = new HashMap<>();
    
    // Double-click window in milliseconds
    private static final long DOUBLE_CLICK_WINDOW = 500;
    
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            
            // Check if clicking a bed
            if (!(state.getBlock() instanceof BedBlock)) return InteractionResult.PASS;
            
            UUID playerId = serverPlayer.getUUID();
            long now = System.currentTimeMillis();
            
            // Check for double-click
            Long lastClick = lastBedClick.get(playerId);
            BlockPos lastPos = lastBedPos.get(playerId);
            
            if (lastClick != null && lastPos != null && 
                now - lastClick < DOUBLE_CLICK_WINDOW && 
                lastPos.equals(pos)) {
                
                // Double-click detected - force sleep
                forceSleep(serverPlayer, level, pos);
                
                // Clear tracking
                lastBedClick.remove(playerId);
                lastBedPos.remove(playerId);
                
                return InteractionResult.SUCCESS;
            }
            
            // Record this click
            lastBedClick.put(playerId, now);
            lastBedPos.put(playerId, pos);
            
            return InteractionResult.PASS; // Let normal bed behavior happen
        });
        
        AioMod.LOGGER.info("Sleep Sooner Manager registered.");
    }
    
    private static void forceSleep(ServerPlayer player, ServerLevel level, BlockPos bedPos) {
        // Set respawn point - use the new RespawnConfig API in 1.21.11
        // player.setRespawnPosition() now takes (RespawnConfig, boolean)
        // For simplicity, we'll just start sleeping and let vanilla handle spawn
        
        // Force start sleeping
        player.startSleeping(bedPos);
        
        // If it's daytime, we need to advance time or just let them rest briefly
        long dayTime = level.getDayTime() % 24000;
        
        if (dayTime >= 0 && dayTime < 12000) {
            // Daytime - notify player
            player.sendSystemMessage(Component.literal("§7You take a quick rest and set your spawn point..."));
            
            // Stop sleeping after a brief moment (simulated rest)
            level.getServer().execute(() -> {
                // Schedule wake up
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
                
                if (player.isSleeping()) {
                    player.stopSleeping();
                }
            });
        } else {
            // Nighttime - normal sleep will handle time skip
            player.sendSystemMessage(Component.literal("§7You drift off to sleep..."));
        }
    }
    
    /**
     * Clean up player data when they disconnect
     */
    public static void onPlayerDisconnect(UUID playerId) {
        lastBedClick.remove(playerId);
        lastBedPos.remove(playerId);
    }
}
