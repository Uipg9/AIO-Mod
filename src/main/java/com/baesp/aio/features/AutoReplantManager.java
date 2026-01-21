package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.rpg.SkillsManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto Replant System
 * 
 * Automatically replants crops when harvested at full maturity.
 * - Works with wheat, carrots, potatoes, beetroot, nether wart
 * - Consumes one seed/item from the drop to replant
 * - Plays planting sound for feedback
 * - Awards Farming skill XP
 * 
 * Inspired by Serilum's Auto Replant mod.
 */
public class AutoReplantManager {
    
    // Map of mature crops to their seed items
    private static final Map<Block, net.minecraft.world.item.Item> CROP_SEEDS = new HashMap<>();
    
    // Crop age properties
    private static final IntegerProperty WHEAT_AGE = CropBlock.AGE;
    private static final IntegerProperty BEETROOT_AGE = BeetrootBlock.AGE;
    private static final IntegerProperty NETHER_WART_AGE = NetherWartBlock.AGE;
    
    static {
        CROP_SEEDS.put(Blocks.WHEAT, Items.WHEAT_SEEDS);
        CROP_SEEDS.put(Blocks.CARROTS, Items.CARROT);
        CROP_SEEDS.put(Blocks.POTATOES, Items.POTATO);
        CROP_SEEDS.put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
        CROP_SEEDS.put(Blocks.NETHER_WART, Items.NETHER_WART);
    }
    
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            
            Block block = state.getBlock();
            
            // Check if this is a supported crop
            if (!CROP_SEEDS.containsKey(block)) return;
            
            // Check if crop is fully mature
            if (!isMature(state)) return;
            
            // Schedule replanting on next tick to allow drops to spawn first
            level.getServer().execute(() -> {
                // Delay slightly to let drops spawn
                tryReplant(level, serverPlayer, pos, block);
            });
        });
        
        AioMod.LOGGER.info("Auto Replant Manager registered.");
    }
    
    private static boolean isMature(BlockState state) {
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
    
    private static void tryReplant(ServerLevel level, ServerPlayer player, BlockPos pos, Block crop) {
        // Get the seed item for this crop
        net.minecraft.world.item.Item seedItem = CROP_SEEDS.get(crop);
        if (seedItem == null) return;
        
        // Check if the position is valid for replanting
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.isAir()) return; // Block wasn't fully broken
        
        // Check if soil is still valid
        BlockPos soilPos = pos.below();
        BlockState soilState = level.getBlockState(soilPos);
        
        boolean validSoil = false;
        if (crop == Blocks.NETHER_WART) {
            validSoil = soilState.is(Blocks.SOUL_SAND);
        } else {
            validSoil = soilState.getBlock() instanceof FarmBlock;
        }
        
        if (!validSoil) return;
        
        // Try to consume a seed from player's inventory
        boolean consumedSeed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == seedItem && stack.getCount() > 0) {
                stack.shrink(1);
                consumedSeed = true;
                break;
            }
        }
        
        if (!consumedSeed) {
            // No seeds available, can't replant
            return;
        }
        
        // Place the crop at age 0
        BlockState newCropState = crop.defaultBlockState();
        level.setBlock(pos, newCropState, Block.UPDATE_ALL);
        
        // Play planting sound
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        
        // Award Farming skill XP
        SkillsManager.addSkillXp(player, com.baesp.aio.rpg.SkillsData.SKILL_FARMING, 2);
    }
    
    /**
     * Check if a block is a supported crop
     */
    public static boolean isSupportedCrop(Block block) {
        return CROP_SEEDS.containsKey(block);
    }
}
