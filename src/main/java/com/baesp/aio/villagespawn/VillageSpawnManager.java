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
        if (!AioMod.CONFIG.villageSpawnEnabled) {
            AioMod.LOGGER.info("Village Spawn Point system disabled in config.");
            return;
        }
        
        // Register world load event to set spawn at village (using Serilum's approach)
        ServerWorldEvents.LOAD.register((server, world) -> {
            AioMod.LOGGER.info("=== ServerWorldEvents.LOAD FIRED ===");
            AioMod.LOGGER.info("World: " + world.dimension());
            AioMod.LOGGER.info("Is Overworld: " + (world == server.overworld()));
            AioMod.LOGGER.info("Has set spawn: " + hasSetSpawn);
            
            if (!hasSetSpawn && world == server.overworld()) {
                AioMod.LOGGER.info("Calling onWorldLoad()...");
                onWorldLoad(world, (ServerLevelData) world.getLevelData());
            } else {
                if (hasSetSpawn) {
                    AioMod.LOGGER.info("Skipped - spawn already set");
                }
                if (world != server.overworld()) {
                    AioMod.LOGGER.info("Skipped - not overworld");
                }
            }
        });
        
        AioMod.LOGGER.info("Village Spawn Point system initialized.");
    }
    
    /**
     * Called when world loads to set spawn at nearest village
     * Based on Serilum's Village Spawn Point implementation
     */
    public static boolean onWorldLoad(ServerLevel serverLevel, ServerLevelData serverLevelData) {
        AioMod.LOGGER.info("=== onWorldLoad() CALLED ===");
        AioMod.LOGGER.info("hasSetSpawn: " + hasSetSpawn);
        
        if (hasSetSpawn) {
            AioMod.LOGGER.info("Spawn already set, returning false");
            return false;
        }
        
        AioMod.LOGGER.info("Finding the nearest village for spawn. This might take a few seconds.");
        BlockPos villagePos = findVillageSpawn(serverLevel, serverLevelData);
        
        AioMod.LOGGER.info("findVillageSpawn() returned: " + (villagePos != null ? villagePos.toShortString() : "null"));
        
        if (villagePos == null) {
            AioMod.LOGGER.warn("No village found within search radius, using default spawn.");
            return false;
        }
        
        AioMod.LOGGER.info("Village found! Setting world spawn to village at: " + villagePos.toShortString());
        
        // Use Serilum's approach: update RespawnData
        net.minecraft.world.level.storage.LevelData.RespawnData oldRespawnData = serverLevel.getRespawnData();
        AioMod.LOGGER.info("Old respawn data - Pos: " + oldRespawnData.pos() + ", Dim: " + oldRespawnData.dimension());
        
        net.minecraft.world.level.storage.LevelData.RespawnData newRespawnData = 
            net.minecraft.world.level.storage.LevelData.RespawnData.of(
                oldRespawnData.dimension(), 
                villagePos, 
                oldRespawnData.yaw(), 
                oldRespawnData.pitch()
            );
        
        AioMod.LOGGER.info("New respawn data - Pos: " + newRespawnData.pos() + ", Dim: " + newRespawnData.dimension());
        
        serverLevel.setRespawnData(newRespawnData);
        hasSetSpawn = true;
        
        AioMod.LOGGER.info("=== Village spawn SUCCESSFULLY SET ===");
        
        return true;
    }
    
    /**
     * Finds a village near spawn and returns its position, or null if not found
     */
    public static BlockPos findVillageSpawn(ServerLevel world, ServerLevelData levelData) {
        AioMod.LOGGER.info("=== findVillageSpawn() CALLED ===");
        AioMod.LOGGER.info("villageSpawnEnabled: " + AioMod.CONFIG.villageSpawnEnabled);
        
        if (!AioMod.CONFIG.villageSpawnEnabled) {
            AioMod.LOGGER.info("Village spawn disabled in config, returning null");
            return null;
        }
        
        try {
            // Get the village structure tag from registry
            var structureRegistry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            AioMod.LOGGER.info("Got structure registry");
            
            var villageTagOptional = structureRegistry.get(StructureTags.VILLAGE);
            AioMod.LOGGER.info("Got village tag optional, present: " + villageTagOptional.isPresent());
            
            if (villageTagOptional.isEmpty()) {
                AioMod.LOGGER.warn("Village structure tag not found in registry!");
                return null;
            }
            
            HolderSet<Structure> villageStructures = villageTagOptional.get();
            AioMod.LOGGER.info("Got village structures holder set, size: " + villageStructures.size());
            
            BlockPos worldSpawn = new BlockPos(0, 64, 0);
            AioMod.LOGGER.info("Searching from spawn position: " + worldSpawn.toShortString());
            
            // Find nearest village structure within 100 chunks (1600 blocks)
            AioMod.LOGGER.info("Calling findNearestMapStructure() - this may take a moment...");
            Pair<BlockPos, net.minecraft.core.Holder<Structure>> result = 
                world.getChunkSource().getGenerator().findNearestMapStructure(
                    world,
                    villageStructures,
                    worldSpawn,
                    100,  // Search radius in chunks
                    false // skipKnownStructures
                );
            
            AioMod.LOGGER.info("findNearestMapStructure() returned: " + (result != null ? "found village" : "null"));
            
            if (result != null) {
                BlockPos villagePos = result.getFirst();
                AioMod.LOGGER.info("Village position from structure: " + villagePos.toShortString());
                
                // Find ground level at village position
                BlockPos groundPos = world.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, villagePos);
                
                AioMod.LOGGER.info("Ground position at village: " + groundPos.toShortString());
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
