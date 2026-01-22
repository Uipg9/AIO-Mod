package com.baesp.aio.warp;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;

import java.util.Set;

/**
 * Home Manager - Handles player home dimension
 * 
 * Features:
 * - Personal peaceful dimension for each player (no hostile mobs)
 * - Beautiful overworld-like terrain with forests, meadows, rivers
 * - Starter cottage built on first arrival
 * - Persists through ascensions
 * - Access via /home command or [ GUI
 */
public class HomeManager {
    
    // Home dimension key
    public static final ResourceKey<Level> PLAYER_HOME = ResourceKey.create(
        Registries.DIMENSION,
        Identifier.parse("aio:player_home")
    );
    
    public static void init() {
        AioMod.LOGGER.info("Home Manager initialized.");
    }
    
    /**
     * Teleport player to their home dimension
     */
    public static boolean teleportToHome(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) return false;
        
        ServerLevel homeWorld = server.getLevel(PLAYER_HOME);
        if (homeWorld == null) {
            player.sendSystemMessage(Component.literal("§cHome dimension not found! Make sure the mod is properly installed."));
            return false;
        }
        
        // Get player's home position (stored in player data)
        var data = PlayerDataManager.getData(player);
        double homeX, homeY, homeZ;
        float homeYaw, homePitch;
        
        if (Double.isNaN(data.homeX)) {
            // First time - create home at a position based on player UUID
            long seed = player.getUUID().getMostSignificantBits();
            int baseX = (int)((seed % 10000) - 5000);
            int baseZ = (int)(((seed >> 16) % 10000) - 5000);
            
            // Force chunk to load and find safe Y
            homeWorld.getChunk(baseX >> 4, baseZ >> 4);
            int safeY = findSafeY(homeWorld, baseX, baseZ);
            
            homeX = baseX + 0.5;
            homeY = safeY + 1.0;
            homeZ = baseZ + 0.5;
            homeYaw = 0;
            homePitch = 0;
            
            // Save the home position
            data.homeX = homeX;
            data.homeY = homeY;
            data.homeZ = homeZ;
            data.homeYaw = homeYaw;
            data.homePitch = homePitch;
            PlayerDataManager.savePlayer(player);
            
            // Build the starter cottage
            buildStarterCottage(homeWorld, new BlockPos(baseX, safeY, baseZ));
            
            player.sendSystemMessage(Component.literal("§a✦ Your home has been created!"));
        } else {
            homeX = data.homeX;
            homeY = data.homeY;
            homeZ = data.homeZ;
            homeYaw = data.homeYaw;
            homePitch = data.homePitch;
        }
        
        // Teleport
        player.teleportTo(
            homeWorld,
            homeX,
            homeY,
            homeZ,
            Set.of(),
            homeYaw,
            homePitch,
            false
        );
        
        player.sendSystemMessage(Component.literal("§a✦ Welcome home!"));
        player.sendSystemMessage(Component.literal("§7This is your personal peaceful dimension."));
        player.sendSystemMessage(Component.literal("§7No hostile mobs will spawn here."));
        
        return true;
    }
    
    /**
     * Find a safe Y coordinate at the given X/Z
     */
    private static int findSafeY(ServerLevel world, int x, int z) {
        // Scan down from max height to find solid ground
        for (int y = 319; y > 50; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            BlockState above = world.getBlockState(pos.above());
            BlockState above2 = world.getBlockState(pos.above(2));
            
            // Skip air/water
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            // Skip non-solid blocks like leaves
            if (!state.isSolid()) continue;
            
            // Make sure there's air above for the player
            if (above.isAir() && above2.isAir()) {
                return y;
            }
        }
        return 64; // Default fallback
    }
    
    /**
     * Set player's home spawn point in the home dimension
     */
    public static boolean setHomeSpawn(ServerPlayer player) {
        if (!isInHome(player)) {
            player.sendSystemMessage(Component.literal("§cYou must be in your home dimension to set your spawn!"));
            return false;
        }
        
        var data = PlayerDataManager.getData(player);
        data.homeX = player.getX();
        data.homeY = player.getY();
        data.homeZ = player.getZ();
        data.homeYaw = player.getYRot();
        data.homePitch = player.getXRot();
        PlayerDataManager.savePlayer(player);
        
        player.sendSystemMessage(Component.literal("§a✦ Home spawn point set!"));
        return true;
    }
    
    /**
     * Build a beautiful starter cottage at the home location
     */
    private static void buildStarterCottage(ServerLevel world, BlockPos center) {
        int y = center.getY();
        int x = center.getX();
        int z = center.getZ();
        
        // Clear area and build foundation
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                // Clear above
                for (int dy = 1; dy <= 10; dy++) {
                    world.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                // Grass foundation
                world.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        
        // Build cottage floor (5x5 interior)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.OAK_PLANKS.defaultBlockState(), 2);
            }
        }
        
        // Cottage walls (2 blocks high)
        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
        BlockState oakPlanks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        
        for (int dy = 1; dy <= 2; dy++) {
            // Corner pillars
            world.setBlock(new BlockPos(x - 2, y + dy, z - 2), oakLog, 2);
            world.setBlock(new BlockPos(x + 2, y + dy, z - 2), oakLog, 2);
            world.setBlock(new BlockPos(x - 2, y + dy, z + 2), oakLog, 2);
            world.setBlock(new BlockPos(x + 2, y + dy, z + 2), oakLog, 2);
            
            // Walls - North/South (with windows in middle)
            for (int dx = -1; dx <= 1; dx++) {
                if (dy == 1 && dx == 0) {
                    // Window
                    world.setBlock(new BlockPos(x + dx, y + dy, z - 2), glass, 2);
                    world.setBlock(new BlockPos(x + dx, y + dy, z + 2), glass, 2);
                } else {
                    world.setBlock(new BlockPos(x + dx, y + dy, z - 2), oakPlanks, 2);
                    world.setBlock(new BlockPos(x + dx, y + dy, z + 2), oakPlanks, 2);
                }
            }
            
            // Walls - East/West
            for (int dz = -1; dz <= 1; dz++) {
                if (dy == 1 && dz == 0) {
                    // Door on East, window on West
                    if (dz == 0) {
                        world.setBlock(new BlockPos(x + 2, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2); // Door opening
                    } else {
                        world.setBlock(new BlockPos(x + 2, y + dy, z + dz), oakPlanks, 2);
                    }
                    world.setBlock(new BlockPos(x - 2, y + dy, z + dz), glass, 2); // Window
                } else {
                    world.setBlock(new BlockPos(x + 2, y + dy, z + dz), oakPlanks, 2);
                    world.setBlock(new BlockPos(x - 2, y + dy, z + dz), oakPlanks, 2);
                }
            }
        }
        
        // Door opening (clear 2 blocks high on east side)
        world.setBlock(new BlockPos(x + 2, y + 1, z), Blocks.AIR.defaultBlockState(), 2);
        world.setBlock(new BlockPos(x + 2, y + 2, z), Blocks.AIR.defaultBlockState(), 2);
        
        // Roof (pyramid style with stairs)
        BlockState stairsN = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockState stairsS = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        BlockState stairsE = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        BlockState stairsW = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState();
        
        // Roof layer 1 (y+3)
        for (int dx = -3; dx <= 3; dx++) {
            world.setBlock(new BlockPos(x + dx, y + 3, z - 3), stairsN, 2);
            world.setBlock(new BlockPos(x + dx, y + 3, z + 3), stairsS, 2);
        }
        for (int dz = -2; dz <= 2; dz++) {
            world.setBlock(new BlockPos(x - 3, y + 3, z + dz), stairsW, 2);
            world.setBlock(new BlockPos(x + 3, y + 3, z + dz), stairsE, 2);
        }
        
        // Roof layer 2 (y+4) - inner roof
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    world.setBlock(new BlockPos(x + dx, y + 4, z + dz), slab, 2);
                }
            }
        }
        
        // Roof center cap
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlock(new BlockPos(x + dx, y + 4, z + dz), oakPlanks, 2);
            }
        }
        
        // Interior decorations
        // Bed (inside near back wall)
        world.setBlock(new BlockPos(x - 1, y + 1, z + 1), Blocks.WHITE_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 2);
        
        // Crafting table
        world.setBlock(new BlockPos(x + 1, y + 1, z + 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        
        // Chest
        world.setBlock(new BlockPos(x + 1, y + 1, z - 1), Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
        
        // Furnace
        world.setBlock(new BlockPos(x - 1, y + 1, z - 1), Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 2);
        
        // Light inside (lantern hanging)
        world.setBlock(new BlockPos(x, y + 3, z), Blocks.LANTERN.defaultBlockState(), 2);
        
        // Path leading to door
        BlockState path = Blocks.DIRT_PATH.defaultBlockState();
        for (int px = 3; px <= 5; px++) {
            world.setBlock(new BlockPos(x + px, y, z), path, 2);
        }
        
        // Flowers around cottage
        BlockState[] flowers = {
            Blocks.POPPY.defaultBlockState(),
            Blocks.DANDELION.defaultBlockState(),
            Blocks.AZURE_BLUET.defaultBlockState(),
            Blocks.OXEYE_DAISY.defaultBlockState(),
            Blocks.CORNFLOWER.defaultBlockState()
        };
        
        int flowerIndex = 0;
        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                if (Math.abs(dx) >= 3 || Math.abs(dz) >= 3) {
                    BlockPos flowerPos = new BlockPos(x + dx, y + 1, z + dz);
                    if (world.getBlockState(flowerPos).isAir()) {
                        world.setBlock(flowerPos, flowers[flowerIndex % flowers.length], 2);
                        flowerIndex++;
                    }
                }
            }
        }
        
        // Torches outside
        world.setBlock(new BlockPos(x + 5, y + 1, z - 1), Blocks.TORCH.defaultBlockState(), 2);
        world.setBlock(new BlockPos(x + 5, y + 1, z + 1), Blocks.TORCH.defaultBlockState(), 2);
        
        // Fence and gate for a small garden area
        for (int dz = -2; dz <= 2; dz++) {
            world.setBlock(new BlockPos(x + 5, y + 1, z + dz), Blocks.OAK_FENCE.defaultBlockState(), 2);
        }
    }
    
    /**
     * Check if player is in their home dimension
     */
    public static boolean isInHome(ServerPlayer player) {
        return player.level().dimension().equals(PLAYER_HOME);
    }
}
