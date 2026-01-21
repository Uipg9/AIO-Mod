package com.baesp.aio.sleepwarp;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SleepWarpManager {
    private static final int DAY_LENGTH_TICKS = 24000;
    private static final Random random = new Random();
    
    public static void init() {
        if (!AioMod.CONFIG.sleepWarpEnabled) {
            AioMod.LOGGER.info("Sleep Warp system disabled in config.");
            return;
        }
        
        ServerTickEvents.END_WORLD_TICK.register(SleepWarpManager::onWorldTick);
        
        AioMod.LOGGER.info("Sleep Warp system initialized.");
    }
    
    private static void onWorldTick(ServerLevel world) {
        if (!AioMod.CONFIG.sleepWarpEnabled) return;
        
        // Only process overworld
        if (world.dimension() != Level.OVERWORLD) return;
        
        // Count sleeping players
        List<ServerPlayer> allPlayers = world.players();
        if (allPlayers.isEmpty()) return;
        
        long sleepingCount = allPlayers.stream()
            .filter(p -> p.isSleeping() && p.getSleepTimer() >= 100)
            .count();
        
        if (sleepingCount == 0) return;
        
        // Calculate warp speed
        int maxTicks = AioMod.CONFIG.sleepWarpMaxTicksAdded;
        double playerMultiplier = AioMod.CONFIG.sleepWarpPlayerMultiplier;
        
        int warpTickCount;
        if (allPlayers.size() == 1) {
            warpTickCount = maxTicks;
        } else {
            double sleepingRatio = (double) sleepingCount / allPlayers.size();
            double scaledRatio = sleepingRatio * playerMultiplier;
            double tickMultiplier = scaledRatio / ((scaledRatio * 2) - playerMultiplier - sleepingRatio + 1);
            warpTickCount = (int) Math.round(maxTicks * Math.max(0.1, Math.min(1.0, tickMultiplier)));
        }
        
        // Check if close to day end
        long worldTime = world.getDayTime() % DAY_LENGTH_TICKS;
        if (worldTime + warpTickCount >= DAY_LENGTH_TICKS) {
            warpTickCount = (int) (DAY_LENGTH_TICKS - worldTime);
        }
        
        if (warpTickCount <= 0) {
            // Wake everyone up
            world.players().forEach(p -> {
                if (p.isSleeping()) {
                    p.stopSleeping();
                }
            });
            if (world.isRaining()) {
                world.setWeatherParameters(0, 0, false, false);
            }
            return;
        }
        
        // Collect valid chunks from player positions
        List<LevelChunk> chunks = new ArrayList<>();
        for (ServerPlayer player : allPlayers) {
            ChunkPos chunkPos = player.chunkPosition();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    LevelChunk chunk = world.getChunk(chunkPos.x + dx, chunkPos.z + dz);
                    if (chunk != null && !chunks.contains(chunk)) {
                        chunks.add(chunk);
                    }
                }
            }
        }
        Collections.shuffle(chunks);
        
        // Get daylight cycle game rule - daylight always enabled in overworld
        boolean doDaylightCycle = true; // Could check via world.getLevelData() if needed
        
        for (int tick = 0; tick < warpTickCount; tick++) {
            // Tick time
            if (doDaylightCycle) {
                world.setDayTime(world.getDayTime() + 1);
            }
            
            // Tick random blocks
            if (AioMod.CONFIG.sleepWarpTickRandomBlocks) {
                for (LevelChunk chunk : chunks) {
                    tickRandomBlocks(world, chunk);
                }
            }
            
            // Tick block entities
            if (AioMod.CONFIG.sleepWarpTickBlockEntities) {
                world.tickBlockEntities();
            }
            
            // Tick precipitation
            if (world.isRaining()) {
                for (LevelChunk chunk : chunks) {
                    if (random.nextInt(16) == 0) {
                        tickPrecipitation(world, chunk);
                    }
                }
                
                // Lightning
                if (AioMod.CONFIG.sleepWarpTickLightning && world.isThundering()) {
                    for (LevelChunk chunk : chunks) {
                        if (random.nextInt(100000) == 0) {
                            spawnLightning(world, chunk);
                        }
                    }
                }
            }
        }
        
        // Send time update packet
        ClientboundSetTimePacket packet = new ClientboundSetTimePacket(
            world.getGameTime(),
            world.getDayTime(),
            doDaylightCycle
        );
        for (ServerPlayer player : allPlayers) {
            player.connection.send(packet);
        }
    }
    
    private static void tickRandomBlocks(ServerLevel world, LevelChunk chunk) {
        int randomTickSpeed = 3; // Default random tick speed
        if (randomTickSpeed <= 0) return;
        
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        
        for (int i = 0; i < randomTickSpeed; i++) {
            int x = startX + random.nextInt(16);
            int z = startZ + random.nextInt(16);
            int y = random.nextInt(world.getHeight());
            
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            
            if (state.isRandomlyTicking()) {
                state.randomTick(world, pos, world.random);
            }
        }
    }
    
    private static void tickPrecipitation(ServerLevel world, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        
        BlockPos topPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        Biome biome = world.getBiome(topPos).value();
        
        // Ice freezing
        if (AioMod.CONFIG.sleepWarpTickIce) {
            BlockPos belowPos = topPos.below();
            if (biome.shouldFreeze(world, belowPos)) {
                world.setBlockAndUpdate(belowPos, Blocks.ICE.defaultBlockState());
            }
        }
        
        // Snow accumulation
        if (AioMod.CONFIG.sleepWarpTickSnow) {
            int maxSnowHeight = 1; // Default max snow height
            if (maxSnowHeight > 0 && biome.shouldSnow(world, topPos)) {
                BlockState topState = world.getBlockState(topPos);
                
                if (topState.isAir()) {
                    world.setBlockAndUpdate(topPos, Blocks.SNOW.defaultBlockState());
                } else if (topState.is(Blocks.SNOW)) {
                    int layers = topState.getValue(SnowLayerBlock.LAYERS);
                    if (layers < Math.min(8, maxSnowHeight)) {
                        world.setBlockAndUpdate(topPos, topState.setValue(SnowLayerBlock.LAYERS, layers + 1));
                    }
                }
            }
        }
    }
    
    private static void spawnLightning(ServerLevel world, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        
        BlockPos strikePos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        
        if (world.isRainingAt(strikePos)) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.setPos(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                lightning.setVisualOnly(false);
                world.addFreshEntity(lightning);
            }
        }
    }
    
    // Called by mixin to suppress vanilla sleep skip
    public static boolean shouldSuppressVanillaSleep() {
        return AioMod.CONFIG.sleepWarpEnabled;
    }
}
