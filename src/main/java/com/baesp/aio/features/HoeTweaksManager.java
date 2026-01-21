package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hoe Tweaks System
 * 
 * Enhances hoe functionality for better farming:
 * - Right-click with hoe to harvest and replant crops in one action
 * - Shift + right-click to till a 3x3 area
 * - Hoes break crop blocks faster
 * 
 * Inspired by Serilum's Hoe Tweaks mod.
 */
public class HoeTweaksManager {
    
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            
            ItemStack heldItem = player.getItemInHand(hand);
            if (!(heldItem.getItem() instanceof HoeItem)) return InteractionResult.PASS;
            
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            
            // Feature 1: Harvest and replant crops
            if (isMatureCrop(state)) {
                harvestAndReplant(level, serverPlayer, pos, state, heldItem);
                return InteractionResult.SUCCESS;
            }
            
            // Feature 2: Area tilling with shift
            if (player.isShiftKeyDown() && canTill(level, pos)) {
                areaIll(level, serverPlayer, pos, heldItem);
                return InteractionResult.SUCCESS;
            }
            
            return InteractionResult.PASS;
        });
        
        AioMod.LOGGER.info("Hoe Tweaks Manager registered.");
    }
    
    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        } else if (block == Blocks.BEETROOTS) {
            return state.getValue(BeetrootBlock.AGE) >= 3;
        } else if (block == Blocks.NETHER_WART) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        
        return false;
    }
    
    private static void harvestAndReplant(ServerLevel level, ServerPlayer player, BlockPos pos, 
                                          BlockState state, ItemStack hoe) {
        Block block = state.getBlock();
        
        // Break the crop (drops items)
        level.destroyBlock(pos, true, player);
        
        // Replant immediately (same block at age 0)
        BlockState newState = block.defaultBlockState();
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        
        // Play sounds
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        
        // Damage hoe slightly
        if (hoe.isDamageableItem()) {
            hoe.hurtAndBreak(1, player, player.getEquipmentSlotForItem(hoe));
        }
    }
    
    private static boolean canTill(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || 
               state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT) ||
               state.is(Blocks.ROOTED_DIRT);
    }
    
    private static void areaIll(ServerLevel level, ServerPlayer player, BlockPos center, ItemStack hoe) {
        int tilled = 0;
        
        // 3x3 area around the clicked block
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos targetPos = center.offset(dx, 0, dz);
                
                if (canTill(level, targetPos) && isAirAbove(level, targetPos)) {
                    // Till this block
                    level.setBlock(targetPos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                    tilled++;
                    
                    // Damage hoe
                    if (hoe.isDamageableItem() && hoe.getDamageValue() < hoe.getMaxDamage() - 1) {
                        hoe.hurtAndBreak(1, player, player.getEquipmentSlotForItem(hoe));
                    }
                }
            }
        }
        
        if (tilled > 0) {
            // Play tilling sound once
            level.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
    
    private static boolean isAirAbove(ServerLevel level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isAir() || above.getBlock() instanceof CropBlock;
    }
}
