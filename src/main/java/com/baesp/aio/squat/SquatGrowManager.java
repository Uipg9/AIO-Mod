package com.baesp.aio.squat;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

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
        
        RandomSource random = serverLevel.getRandom();
        
        // Iterate over nearby blocks
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -1; dy <= 2; dy++) {  // Increased dy range for taller plants
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    Block block = state.getBlock();
                    
                    // Handle sugarcane growth
                    if (block == Blocks.SUGAR_CANE) {
                        if (random.nextDouble() < chance) {
                            growSugarcane(serverLevel, pos, random, multiplier);
                        }
                        continue;
                    }
                    
                    // Handle pumpkin/melon stem acceleration
                    if (block == Blocks.PUMPKIN_STEM || block == Blocks.MELON_STEM) {
                        if (random.nextDouble() < chance) {
                            accelerateStemGrowth(serverLevel, pos, state, random, multiplier);
                        }
                        continue;
                    }
                    
                    // Check if it's a standard growable block (crops, etc.)
                    if (block instanceof BonemealableBlock growable) {
                        try {
                            if (growable.isValidBonemealTarget(serverLevel, pos, state)) {
                                // Apply growth chance
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
                        } catch (Exception e) {
                            // Skip this block if any error occurs (block doesn't support growth operation)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Grows sugarcane by adding blocks on top if possible
     */
    private static void growSugarcane(ServerLevel level, BlockPos pos, RandomSource random, int multiplier) {
        // Find the top of the sugarcane
        BlockPos topPos = pos;
        while (level.getBlockState(topPos.above()).is(Blocks.SUGAR_CANE)) {
            topPos = topPos.above();
        }
        
        // Count current height
        BlockPos bottomPos = pos;
        while (level.getBlockState(bottomPos.below()).is(Blocks.SUGAR_CANE)) {
            bottomPos = bottomPos.below();
        }
        int height = topPos.getY() - bottomPos.getY() + 1;
        
        // Sugarcane can grow up to 3 blocks
        if (height < 3) {
            BlockPos aboveTop = topPos.above();
            if (level.getBlockState(aboveTop).isAir()) {
                for (int i = 0; i < multiplier && height + i < 3; i++) {
                    BlockPos growPos = topPos.above(i + 1);
                    if (level.getBlockState(growPos).isAir()) {
                        level.setBlock(growPos, Blocks.SUGAR_CANE.defaultBlockState(), 3);
                    } else {
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Accelerates pumpkin/melon stem growth and fruit generation
     */
    private static void accelerateStemGrowth(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, int multiplier) {
        Block block = state.getBlock();
        
        // Safety check - ensure this is actually a stem block
        if (!(block instanceof StemBlock)) {
            return;
        }
        
        // Get stem age safely
        try {
            IntegerProperty ageProperty = StemBlock.AGE;
            if (!state.hasProperty(ageProperty)) {
                return; // Block doesn't have the age property
            }
            int age = state.getValue(ageProperty);
        
        // If stem isn't fully grown, grow it
        if (age < 7) {
            int newAge = Math.min(7, age + multiplier);
            level.setBlock(pos, state.setValue(ageProperty, newAge), 3);
        } else {
            // Stem is mature - try to spawn fruit
            Block fruitBlock = (block == Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : Blocks.MELON;
            
            // Check adjacent positions for fruit placement
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos fruitPos = pos.relative(dir);
                BlockState groundState = level.getBlockState(fruitPos.below());
                
                // Check if position is valid for fruit
                if (level.getBlockState(fruitPos).isAir() && 
                    (groundState.is(Blocks.FARMLAND) || groundState.is(Blocks.DIRT) || 
                     groundState.is(Blocks.GRASS_BLOCK) || groundState.is(Blocks.COARSE_DIRT) ||
                     groundState.is(Blocks.ROOTED_DIRT) || groundState.is(Blocks.PODZOL) ||
                     groundState.is(Blocks.MOSS_BLOCK))) {
                    
                    // Random chance to spawn fruit
                    if (random.nextInt(4) == 0) {  // 25% chance per direction
                        level.setBlock(fruitPos, fruitBlock.defaultBlockState(), 3);
                        return; // Only spawn one fruit per squat
                    }
                }
            }
        }
        } catch (Exception e) {
            // Silently ignore - block state doesn't support this property
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
