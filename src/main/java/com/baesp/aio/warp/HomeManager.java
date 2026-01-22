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

import java.util.Set;

/**
 * Home Manager - Handles player home dimension
 * 
 * Features:
 * - Personal peaceful dimension for each player
 * - Plains grassland with no hostile mob spawning
 * - Persists through ascensions
 * - Access via /home command or [ GUI
 */
public class HomeManager {
    
    // Home dimension key
    public static final ResourceKey<Level> PLAYER_HOME = ResourceKey.create(
        Registries.DIMENSION,
        Identifier.parse("aio:player_home")
    );
    
    private static final int HOME_Y = 5; // Ground level for flat world (bedrock + 3 dirt + 1 grass)
    
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
            // First time - create home at a random position based on player UUID
            // This ensures each player gets their own area
            long seed = player.getUUID().getMostSignificantBits();
            homeX = ((seed % 10000) - 5000) + 0.5;
            homeZ = (((seed >> 16) % 10000) - 5000) + 0.5;
            homeY = HOME_Y + 1;
            homeYaw = 0;
            homePitch = 0;
            
            // Save the home position
            data.homeX = homeX;
            data.homeY = homeY;
            data.homeZ = homeZ;
            data.homeYaw = homeYaw;
            data.homePitch = homePitch;
            PlayerDataManager.savePlayer(player);
            
            // Build a small starter platform
            buildHomePlatform(homeWorld, new BlockPos((int)homeX, HOME_Y, (int)homeZ));
            
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
        player.sendSystemMessage(Component.literal("§7No hostile mobs will spawn here naturally."));
        
        return true;
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
     * Build a small starter platform at the home location
     */
    private static void buildHomePlatform(ServerLevel world, BlockPos center) {
        // Build a 5x5 platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.offset(x, 0, z);
                world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        
        // Add some decorations
        world.setBlock(center.offset(2, 1, 2), Blocks.OAK_FENCE.defaultBlockState(), 2);
        world.setBlock(center.offset(-2, 1, 2), Blocks.OAK_FENCE.defaultBlockState(), 2);
        world.setBlock(center.offset(2, 1, -2), Blocks.OAK_FENCE.defaultBlockState(), 2);
        world.setBlock(center.offset(-2, 1, -2), Blocks.OAK_FENCE.defaultBlockState(), 2);
        
        // Torches on fences
        world.setBlock(center.offset(2, 2, 2), Blocks.TORCH.defaultBlockState(), 2);
        world.setBlock(center.offset(-2, 2, 2), Blocks.TORCH.defaultBlockState(), 2);
        world.setBlock(center.offset(2, 2, -2), Blocks.TORCH.defaultBlockState(), 2);
        world.setBlock(center.offset(-2, 2, -2), Blocks.TORCH.defaultBlockState(), 2);
    }
    
    /**
     * Check if player is in their home dimension
     */
    public static boolean isInHome(ServerPlayer player) {
        return player.level().dimension().equals(PLAYER_HOME);
    }
}
