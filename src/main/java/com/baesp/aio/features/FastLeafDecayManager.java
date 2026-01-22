package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast Leaf Decay System
 * 
 * When a log is broken, nearby leaves will decay much faster than vanilla.
 * This makes tree farming much more convenient.
 */
public class FastLeafDecayManager {
    
    private static final int DECAY_RANGE = 6;  // Range to check for leaves
    private static final int DECAY_DELAY = 2;  // Ticks between decay checks
    
    public static void register() {
        // When a log is broken, schedule nearby leaves for fast decay
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return;
            if (!(player instanceof ServerPlayer)) return;
            
            String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            
            // Check if a log was broken
            if (blockName.contains("log") || blockName.contains("stem") || 
                blockName.contains("wood") || blockName.contains("hyphae")) {
                // Schedule fast leaf decay for nearby leaves
                scheduleLeafDecay(level, pos);
            }
        });
        
        AioMod.LOGGER.info("Fast Leaf Decay Manager registered.");
    }
    
    private static void scheduleLeafDecay(ServerLevel level, BlockPos logPos) {
        // Find all leaves in range and schedule them for decay
        List<BlockPos> leavesToDecay = new ArrayList<>();
        
        for (int dx = -DECAY_RANGE; dx <= DECAY_RANGE; dx++) {
            for (int dy = -DECAY_RANGE; dy <= DECAY_RANGE; dy++) {
                for (int dz = -DECAY_RANGE; dz <= DECAY_RANGE; dz++) {
                    BlockPos checkPos = logPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.getBlock() instanceof LeavesBlock) {
                        // Check if this is a natural leaf (not player-placed)
                        if (state.hasProperty(LeavesBlock.PERSISTENT) && 
                            !state.getValue(LeavesBlock.PERSISTENT)) {
                            leavesToDecay.add(checkPos.immutable());
                        }
                    }
                }
            }
        }
        
        // Schedule decay with staggered timing for natural look
        int delay = DECAY_DELAY;
        for (BlockPos leafPos : leavesToDecay) {
            final int currentDelay = delay;
            level.scheduleTick(leafPos, level.getBlockState(leafPos).getBlock(), currentDelay);
            delay += 1 + level.random.nextInt(2); // Stagger by 1-2 ticks each
        }
    }
}
