package com.baesp.aio.villagespawn;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ServerLevelData;
import com.mojang.datafixers.util.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Village Spawn Manager - teleports new players to nearest village
 * Based on Serilum's Village Spawn Point implementation
 */
public class VillageSpawnManager {
    
    private static boolean hasSetWorldSpawn = false;
    private static BlockPos cachedVillagePos = null;
    private static final Set<UUID> teleportedPlayers = new HashSet<>();
    // Per-player village spawn points (for ascended players)
    private static final java.util.Map<UUID, BlockPos> playerVillageSpawns = new java.util.HashMap<>();
    
    public static void init() {
        if (!AioMod.CONFIG.villageSpawnEnabled) {
            AioMod.LOGGER.info("Village Spawn Point system disabled in config.");
            return;
        }
        
        // Find village on world load and set world spawn
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!hasSetWorldSpawn && world == server.overworld()) {
                AioMod.LOGGER.info("Finding village for world spawn...");
                BlockPos villagePos = findVillageSpawn(world, (ServerLevelData) world.getLevelData());
                
                if (villagePos != null) {
                    // Force chunk to generate/load before checking height
                    world.getChunk(villagePos.getX() >> 4, villagePos.getZ() >> 4);
                    
                    // Find safe Y from ground up - don't trust heightmap alone
                    int safeY = findSafeY(world, villagePos.getX(), villagePos.getZ());
                    BlockPos safePos = new BlockPos(villagePos.getX(), safeY + 1, villagePos.getZ());
                    
                    cachedVillagePos = safePos;
                    // Set the WORLD spawn to village using RespawnData
                    net.minecraft.world.level.storage.LevelData.RespawnData respawnData = 
                        net.minecraft.world.level.storage.LevelData.RespawnData.of(
                            world.dimension(),
                            safePos,
                            0.0f,
                            0.0f
                        );
                    world.setRespawnData(respawnData);
                    AioMod.LOGGER.info("Set world spawn to village at SAFE position: " + safePos.getX() + ", " + safePos.getY() + ", " + safePos.getZ());
                    hasSetWorldSpawn = true;
                }
            }
        });
        
        // Teleport new players to village on first join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID playerId = player.getUUID();
            
            // Load player's saved ascension spawn (if they've ascended before)
            var playerData = com.baesp.aio.data.PlayerDataManager.getData(player);
            if (playerData != null && playerData.ascendancy.ascensionCount > 0 
                    && !Double.isNaN(playerData.ascendancy.ascensionSpawnX)) {
                // Player has ascended before - restore their village spawn
                BlockPos savedVillageSpawn = new BlockPos(
                    (int) playerData.ascendancy.ascensionSpawnX,
                    (int) playerData.ascendancy.ascensionSpawnY,
                    (int) playerData.ascendancy.ascensionSpawnZ
                );
                playerVillageSpawns.put(playerId, savedVillageSpawn);
                AioMod.LOGGER.info("Restored ascension village spawn for " + player.getName().getString() + " at " + savedVillageSpawn.getX() + ", " + savedVillageSpawn.getY() + ", " + savedVillageSpawn.getZ());
            }
            
            // Only teleport if we haven't teleported this player before AND they're a new player
            if (!teleportedPlayers.contains(playerId)) {
                teleportedPlayers.add(playerId);
                
                ServerLevel overworld = server.overworld();
                BlockPos spawnPos = cachedVillagePos;
                
                if (spawnPos == null) {
                    // Try to find village now
                    spawnPos = findVillageSpawn(overworld, (ServerLevelData) overworld.getLevelData());
                }
                
                if (spawnPos != null) {
                    final BlockPos finalPos = spawnPos;
                    // Delay teleport to ensure chunks are loaded
                    server.execute(() -> {
                        // Force load the chunk at the village position
                        overworld.getChunk(finalPos.getX() >> 4, finalPos.getZ() >> 4);
                        
                        // Find a SAFE spawn position - scan upward from Y=60 to find air
                        int safeY = findSafeY(overworld, finalPos.getX(), finalPos.getZ());
                        
                        player.teleportTo(
                            overworld,
                            finalPos.getX() + 0.5,
                            safeY + 1.0,
                            finalPos.getZ() + 0.5,
                            java.util.Set.of(),
                            player.getYRot(),
                            player.getXRot(),
                            false
                        );
                        AioMod.LOGGER.info("Teleported " + player.getName().getString() + " to village at " + finalPos.getX() + ", " + (safeY + 1) + ", " + finalPos.getZ());
                    });
                }
            }
        });
        
        // Handle player respawn - teleport to village if no bed spawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Only handle death respawns (not end portal returns)
            if (!alive) {
                // Check for player-specific village spawn (from ascension)
                BlockPos villagePos = playerVillageSpawns.get(newPlayer.getUUID());
                if (villagePos == null) {
                    villagePos = cachedVillagePos; // Fall back to world village
                }
                
                if (villagePos == null) return;
                
                ServerLevel overworld = ((ServerLevel) newPlayer.level()).getServer().overworld();
                final BlockPos finalVillagePos = villagePos;
                
                // Check if player is already near their village (might have bed there)
                double distToVillage = newPlayer.position().distanceTo(
                    new net.minecraft.world.phys.Vec3(finalVillagePos.getX(), finalVillagePos.getY(), finalVillagePos.getZ())
                );
                
                // Only teleport if they're far from their village (more than 100 blocks)
                // This prevents teleporting players who set bed spawn at village
                if (distToVillage > 100) {
                    ((ServerLevel) newPlayer.level()).getServer().execute(() -> {
                        // Force load chunk
                        overworld.getChunk(finalVillagePos.getX() >> 4, finalVillagePos.getZ() >> 4);
                        
                        int safeY = findSafeY(overworld, finalVillagePos.getX(), finalVillagePos.getZ());
                        
                        newPlayer.teleportTo(
                            overworld,
                            finalVillagePos.getX() + 0.5,
                            safeY + 1.0,
                            finalVillagePos.getZ() + 0.5,
                            java.util.Set.of(),
                            newPlayer.getYRot(),
                            newPlayer.getXRot(),
                            false
                        );
                        AioMod.LOGGER.info("Respawned " + newPlayer.getName().getString() + " at their village: " + finalVillagePos.getX() + ", " + (safeY + 1) + ", " + finalVillagePos.getZ());
                    });
                }
            }
        });
        
        AioMod.LOGGER.info("Village Spawn Point system initialized.");
    }
    
    /**
     * Finds a safe Y coordinate by scanning DOWN from max height to find surface
     * This ensures we find the actual surface, not cave ceilings
     */
    private static int findSafeY(ServerLevel world, int x, int z) {
        // Force chunk to be fully loaded/generated
        var chunk = world.getChunk(x >> 4, z >> 4);
        AioMod.LOGGER.info("Finding safe Y at " + x + ", " + z + " - chunk status: " + chunk.getPersistedStatus());
        
        // Scan DOWN from top to find the first solid block (the actual surface)
        for (int y = 319; y > 50; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockPos abovePos = new BlockPos(x, y + 1, z);
            BlockPos above2Pos = new BlockPos(x, y + 2, z);
            
            var blockState = world.getBlockState(checkPos);
            var aboveState = world.getBlockState(abovePos);
            var above2State = world.getBlockState(above2Pos);
            
            // Skip air/water - we're looking for ground
            if (blockState.isAir() || !blockState.getFluidState().isEmpty()) {
                continue;
            }
            
            // Skip leaves and other non-solid blocks
            if (!blockState.isSolid()) {
                continue;
            }
            
            // Check if we found solid ground with 2 air blocks above (standing space)
            boolean abovePassable = aboveState.isAir() || (!aboveState.isSolid() && aboveState.getFluidState().isEmpty());
            boolean above2Passable = above2State.isAir() || (!above2State.isSolid() && above2State.getFluidState().isEmpty());
            
            if (abovePassable && above2Passable) {
                AioMod.LOGGER.info("Found safe Y at: " + y + " (block: " + blockState.getBlock().getName().getString() + ")");
                return y;
            }
        }
        
        // Fallback - try the heightmap (should be reliable for surface)
        BlockPos heightmapPos = world.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
            new BlockPos(x, 64, z)
        );
        int heightmapY = heightmapPos.getY();
        AioMod.LOGGER.info("Fallback using WORLD_SURFACE heightmap Y: " + heightmapY);
        if (heightmapY > 50 && heightmapY < 320) {
            return heightmapY;
        }
        
        // Ultimate fallback to Y=100 if nothing found (should be safe above any terrain)
        AioMod.LOGGER.warn("Could not find safe Y at " + x + ", " + z + " - defaulting to 100");
        return 100;
    }
    
    /**
     * Finds a village near spawn and returns its position, or null if not found
     */
    public static BlockPos findVillageSpawn(ServerLevel world, ServerLevelData levelData) {
        if (!AioMod.CONFIG.villageSpawnEnabled) {
            return null;
        }
        
        try {
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
                    100,
                    false
                );
            
            if (result != null) {
                BlockPos villagePos = result.getFirst();
                // Find safe ground level at village position
                BlockPos groundPos = world.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 
                    villagePos
                );
                AioMod.LOGGER.info("Found village at: " + villagePos.getX() + ", " + groundPos.getY() + ", " + villagePos.getZ());
                return groundPos;
            }
        } catch (Exception e) {
            AioMod.LOGGER.error("Error finding village spawn: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Finds a DISTANT village for ascension (at least 5000 blocks from origin)
     */
    public static BlockPos findDistantVillage(ServerLevel world) {
        try {
            var structureRegistry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var villageTagOptional = structureRegistry.get(StructureTags.VILLAGE);
            
            if (villageTagOptional.isEmpty()) {
                AioMod.LOGGER.warn("Village tag not found for distant village search");
                return null;
            }
            
            HolderSet<Structure> villageStructures = villageTagOptional.get();
            
            // Search from a random VERY distant point to find a village far from spawn
            java.util.Random rand = new java.util.Random();
            double angle = rand.nextDouble() * 2 * Math.PI;
            int distance = 5000 + rand.nextInt(3000); // 5000-8000 blocks away!
            int searchX = (int)(Math.cos(angle) * distance);
            int searchZ = (int)(Math.sin(angle) * distance);
            BlockPos searchFrom = new BlockPos(searchX, 64, searchZ);
            
            AioMod.LOGGER.info("Searching for distant village near: " + searchX + ", " + searchZ + " (distance: " + distance + ")");
            
            Pair<BlockPos, net.minecraft.core.Holder<Structure>> result = 
                world.getChunkSource().getGenerator().findNearestMapStructure(
                    world,
                    villageStructures,
                    searchFrom,
                    100,  // Larger radius to ensure we find one
                    false
                );
            
            if (result != null) {
                BlockPos villagePos = result.getFirst();
                BlockPos groundPos = world.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 
                    villagePos
                );
                AioMod.LOGGER.info("Found distant village at: " + groundPos.getX() + ", " + groundPos.getY() + ", " + groundPos.getZ());
                return groundPos;
            } else {
                AioMod.LOGGER.warn("No distant village found near " + searchX + ", " + searchZ);
            }
        } catch (Exception e) {
            AioMod.LOGGER.error("Error finding distant village: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Sets the player's personal village spawn point (used after ascension)
     * This overrides the world village spawn for this specific player
     */
    public static void setPlayerVillageSpawn(UUID playerId, BlockPos villagePos) {
        playerVillageSpawns.put(playerId, villagePos);
        AioMod.LOGGER.info("Set player " + playerId + " village spawn to: " + villagePos.getX() + ", " + villagePos.getY() + ", " + villagePos.getZ());
    }
    
    /**
     * Gets the player's current village spawn point (player-specific or world default)
     */
    public static BlockPos getPlayerVillageSpawn(UUID playerId) {
        BlockPos playerSpawn = playerVillageSpawns.get(playerId);
        return playerSpawn != null ? playerSpawn : cachedVillagePos;
    }
}
