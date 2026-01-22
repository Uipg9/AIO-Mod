package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.ascendancy.AscendancyManager;
import com.baesp.aio.rpg.SkillsManager;
import com.baesp.aio.rpg.SkillsData;
import com.baesp.aio.rpg.economy.EconomyManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Right-Click Harvest System
 * 
 * Allows right-clicking mature crops to harvest and automatically replant.
 * - Works with wheat, carrots, potatoes, beetroot, nether wart, cocoa
 * - Drops items directly to player
 * - Automatically replants the crop at age 0
 * - Awards Farming skill XP
 */
public class RightClickHarvestManager {
    
    // Map of crops to their seed items for replanting
    private static final Map<Block, net.minecraft.world.item.Item> CROP_SEEDS = new HashMap<>();
    
    static {
        CROP_SEEDS.put(Blocks.WHEAT, Items.WHEAT_SEEDS);
        CROP_SEEDS.put(Blocks.CARROTS, Items.CARROT);
        CROP_SEEDS.put(Blocks.POTATOES, Items.POTATO);
        CROP_SEEDS.put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
        CROP_SEEDS.put(Blocks.NETHER_WART, Items.NETHER_WART);
    }
    
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            
            // Check if this is a supported crop
            if (!isSupportedCrop(block)) return InteractionResult.PASS;
            
            // Check if crop is fully mature
            if (!isMature(state)) return InteractionResult.PASS;
            
            // Harvest and replant!
            harvestAndReplant((ServerLevel) world, serverPlayer, pos, state, block);
            
            return InteractionResult.SUCCESS;
        });
        
        AioMod.LOGGER.info("Right-Click Harvest Manager registered.");
    }
    
    private static boolean isSupportedCrop(Block block) {
        return CROP_SEEDS.containsKey(block) || block == Blocks.COCOA;
    }
    
    private static boolean isMature(BlockState state) {
        Block block = state.getBlock();
        
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        } else if (block == Blocks.BEETROOTS) {
            return state.getValue(BeetrootBlock.AGE) >= 3;
        } else if (block == Blocks.NETHER_WART) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        } else if (block == Blocks.COCOA) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        return false;
    }
    
    private static void harvestAndReplant(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, Block block) {
        // Get the drops from the crop
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        
        // Give drops to player (minus one seed for replanting)
        net.minecraft.world.item.Item seedItem = CROP_SEEDS.get(block);
        boolean usedSeedForReplant = false;
        
        for (ItemStack drop : drops) {
            if (!usedSeedForReplant && seedItem != null && drop.is(seedItem)) {
                // Use one for replanting
                if (drop.getCount() > 1) {
                    drop.shrink(1);
                    player.getInventory().placeItemBackInInventory(drop);
                }
                usedSeedForReplant = true;
            } else {
                player.getInventory().placeItemBackInInventory(drop);
            }
        }
        
        // Replant the crop at age 0
        if (block instanceof CropBlock) {
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        } else if (block == Blocks.BEETROOTS) {
            level.setBlock(pos, Blocks.BEETROOTS.defaultBlockState(), Block.UPDATE_ALL);
        } else if (block == Blocks.NETHER_WART) {
            level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState(), Block.UPDATE_ALL);
        } else if (block == Blocks.COCOA) {
            // Cocoa needs to keep its facing direction
            level.setBlock(pos, state.setValue(CocoaBlock.AGE, 0), Block.UPDATE_ALL);
        }
        
        // Play harvest sound
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        
        // Spawn particles for visual feedback
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        // Green happy villager particles for successful harvest
        level.sendParticles(
            net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
            x, y, z,
            8,     // count
            0.3,   // x spread
            0.3,   // y spread  
            0.3,   // z spread
            0.0    // speed
        );
        
        // Also some crop-like particles
        level.sendParticles(
            net.minecraft.core.particles.ParticleTypes.COMPOSTER,
            x, y + 0.2, z,
            5,
            0.2, 0.1, 0.2,
            0.02
        );
        
        // Award farming XP (skill XP)
        SkillsManager.addSkillXp(player, SkillsData.SKILL_FARMING, AioMod.CONFIG.xpPerSkillAction);
        
        // Award Soul XP for farming
        AscendancyManager.addSoulXp(player, AioMod.CONFIG.rightClickHarvestSoulXp);
        
        // Award money for harvesting
        long moneyReward = AioMod.CONFIG.rightClickHarvestMoney;
        if (moneyReward > 0) {
            EconomyManager.deposit(player, moneyReward);
        }
    }
}
