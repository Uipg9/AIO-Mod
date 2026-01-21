package com.baesp.aio.squat;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.util.RandomSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SquatGrowManager {
    
    private static final Set<UUID> playersWithSquatGrowEnabled = new HashSet<>();
    
    public static void init() {
        if (!AioMod.CONFIG.squatGrowEnabled) {
            AioMod.LOGGER.info("Squat Grow system disabled in config.");
            return;
        }
        
        AioMod.LOGGER.info("Squat Grow system initialized.");
    }
    
    public static void onPlayerCrouch(ServerPlayer player) {
        performSquatGrow(player);
    }
    
    public static void performSquatGrow(ServerPlayer player) {
        if (!AioMod.CONFIG.squatGrowEnabled) return;
        if (!isSquatGrowEnabled(player)) return;
        
        Level level = player.level();
        if (level.isClientSide()) return;
        
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos playerPos = player.blockPosition();
        int range = AioMod.CONFIG.squatGrowRange;
        int multiplier = AioMod.CONFIG.squatGrowMultiplier;
        double chance = AioMod.CONFIG.squatGrowChance;
        
        // Iterate over nearby blocks
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    Block block = state.getBlock();
                    
                    // Check if it's a growable block
                    if (block instanceof BonemealableBlock growable) {
                        if (growable.isValidBonemealTarget(serverLevel, pos, state)) {
                            // Apply growth chance
                            RandomSource random = serverLevel.getRandom();
                            if (random.nextDouble() < chance) {
                                // Apply multiple growth attempts based on multiplier
                                for (int i = 0; i < multiplier; i++) {
                                    if (growable.isBonemealSuccess(serverLevel, random, pos, state)) {
                                        growable.performBonemeal(serverLevel, random, pos, state);
                                        // Refresh state
                                        state = serverLevel.getBlockState(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static void toggleSquatGrow(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (playersWithSquatGrowEnabled.contains(uuid)) {
            playersWithSquatGrowEnabled.remove(uuid);
        } else {
            playersWithSquatGrowEnabled.add(uuid);
        }
        // Save to player data
        PlayerDataManager.getData(player).squatGrowEnabled = playersWithSquatGrowEnabled.contains(uuid);
    }
    
    public static boolean isSquatGrowEnabled(ServerPlayer player) {
        // Check player data
        return PlayerDataManager.getData(player).squatGrowEnabled;
    }
    
    public static void loadSquatGrowState(ServerPlayer player) {
        if (PlayerDataManager.getData(player).squatGrowEnabled) {
            playersWithSquatGrowEnabled.add(player.getUUID());
        }
    }
}
