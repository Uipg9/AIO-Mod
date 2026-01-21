package com.baesp.aio.villagespawn;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ServerLevelData;
import com.mojang.datafixers.util.Pair;

/**
 * Village Spawn Manager - sets world spawn to nearest village
 * Based on Serilum's Village Spawn Point implementation
 */
public class VillageSpawnManager {
    
    private static boolean hasSetSpawn = false;
    
    public static void init() {
        // TODO: Village spawn is temporarily disabled - need to find correct 1.21.11 API
        // The setDefaultSpawnPos / setSpawn methods don't exist in current Minecraft version
        // Will research and implement properly in next version
        AioMod.LOGGER.warn("Village Spawn Point system temporarily disabled - API research needed.");
        return;
        
        /*
        if (!AioMod.CONFIG.villageSpawnEnabled) {
            AioMod.LOGGER.info("Village Spawn Point system disabled in config.");
            return;
        }
        
        // Register world load event to set spawn at village
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!hasSetSpawn && world == server.overworld()) {
                BlockPos villagePos = findVillageSpawn(world, (ServerLevelData) world.getLevelData());
                
                if (villagePos != null) {
                    // Set the shared spawn position for the overworld
                    world.setDefaultSpawnPos(villagePos, 0.0f);
                    hasSetSpawn = true;
                    AioMod.LOGGER.info("Successfully set world spawn to village at: " + villagePos.toShortString());
                } else {
                    AioMod.LOGGER.warn("Could not find village for spawn, using default spawn.");
                }
            }
        });
        
        AioMod.LOGGER.info("Village Spawn Point system initialized.");
        */
    }
    
    /**
     * Finds a village near spawn and returns its position, or null if not found
     */
    public static BlockPos findVillageSpawn(ServerLevel world, ServerLevelData levelData) {
        if (!AioMod.CONFIG.villageSpawnEnabled) return null;
        
        try {
            // Get the village structure tag from registry
            var structureRegistry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var villageTagOptional = structureRegistry.get(StructureTags.VILLAGE);
            
            if (villageTagOptional.isEmpty()) {
                AioMod.LOGGER.warn("Village structure tag not found in registry!");
                return null;
            }
            
            HolderSet<Structure> villageStructures = villageTagOptional.get();
            BlockPos worldSpawn = new BlockPos(0, 64, 0);
            
            // Find nearest village structure within 100 chunks (1600 blocks)
            Pair<BlockPos, net.minecraft.core.Holder<Structure>> result = 
                world.getChunkSource().getGenerator().findNearestMapStructure(
                    world,
                    villageStructures,
                    worldSpawn,
                    100,  // Search radius in chunks
                    false // skipKnownStructures
                );
            
            if (result != null) {
                BlockPos villagePos = result.getFirst();
                // Find ground level at village position
                BlockPos groundPos = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, villagePos);
                
                AioMod.LOGGER.info("World spawn set to village at: " + groundPos);
                return groundPos;
            } else {
                AioMod.LOGGER.info("No village found within 1600 blocks, using default spawn.");
                return null;
            }
        } catch (Exception e) {
            AioMod.LOGGER.error("Error finding village spawn: " + e.getMessage(), e);
            return null;
        }
    }
}
