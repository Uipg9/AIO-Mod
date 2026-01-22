package com.baesp.aio.warp;

import com.baesp.aio.AioMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Warp Manager - Handles player warps and the Warp Hub dimension
 * 
 * INSPIRATION CREDITS:
 * - Waystones by BlayTheNinth - Inspired the concept of named teleportation points
 * - Fabric Dimensions API - Used for dimension teleportation patterns
 * - Misode's Dimension Generator - Used for understanding JSON dimension format
 * - Minecraft Wiki Custom Dimensions - Reference for dimension_type structure
 * 
 * Features:
 * - Personal warp points that players can set and name
 * - Warp Hub dimension with portals for each warp
 * - Auto-saves previous location when entering hub
 * - "Previous Location" portal to return where you came from
 */
public class WarpManager {
    
    // Warp Hub dimension key
    public static final ResourceKey<Level> WARP_HUB = ResourceKey.create(
        Registries.DIMENSION,
        Identifier.parse("aio:warp_hub")
    );
    
    // Player warps: UUID -> List of Warps
    private static final Map<UUID, List<WarpPoint>> PLAYER_WARPS = new ConcurrentHashMap<>();
    
    // Previous locations (before entering warp hub)
    private static final Map<UUID, PreviousLocation> PREVIOUS_LOCATIONS = new ConcurrentHashMap<>();
    
    private static Path DATA_DIR;
    
    public static void init() {
        AioMod.LOGGER.info("Warp Manager initialized.");
    }
    
    public static void onServerStart(MinecraftServer server) {
        DATA_DIR = FabricLoader.getInstance().getGameDir()
            .resolve("world")
            .resolve("aio_data")
            .resolve("warps");
        try {
            Files.createDirectories(DATA_DIR);
            loadAllWarps();
        } catch (IOException e) {
            AioMod.LOGGER.error("Failed to create warp data directory", e);
        }
    }
    
    public static void onServerStop() {
        saveAllWarps();
    }
    
    /**
     * Get all warps for a player
     */
    public static List<WarpPoint> getWarps(ServerPlayer player) {
        return PLAYER_WARPS.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>());
    }
    
    /**
     * Add a new warp point for a player
     */
    public static boolean addWarp(ServerPlayer player, String name) {
        List<WarpPoint> warps = getWarps(player);
        
        // Check for duplicate name
        for (WarpPoint warp : warps) {
            if (warp.name.equalsIgnoreCase(name)) {
                player.sendSystemMessage(Component.literal("§cA warp with that name already exists!"));
                return false;
            }
        }
        
        // Create the warp - store dimension as the ResourceKey string form
        WarpPoint warp = new WarpPoint(
            name,
            player.level().dimension().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
        
        warps.add(warp);
        savePlayerWarps(player.getUUID());
        
        player.sendSystemMessage(Component.literal("§a✦ Warp '" + name + "' created at your location!"));
        return true;
    }
    
    /**
     * Remove a warp by name
     */
    public static boolean removeWarp(ServerPlayer player, String name) {
        List<WarpPoint> warps = getWarps(player);
        
        for (int i = 0; i < warps.size(); i++) {
            if (warps.get(i).name.equalsIgnoreCase(name)) {
                warps.remove(i);
                savePlayerWarps(player.getUUID());
                player.sendSystemMessage(Component.literal("§c✦ Warp '" + name + "' deleted!"));
                return true;
            }
        }
        
        player.sendSystemMessage(Component.literal("§cNo warp found with that name!"));
        return false;
    }
    
    /**
     * Rename a warp
     */
    public static boolean renameWarp(ServerPlayer player, String oldName, String newName) {
        List<WarpPoint> warps = getWarps(player);
        
        // Check if new name already exists
        for (WarpPoint warp : warps) {
            if (warp.name.equalsIgnoreCase(newName)) {
                player.sendSystemMessage(Component.literal("§cA warp with that name already exists!"));
                return false;
            }
        }
        
        // Find and rename
        for (WarpPoint warp : warps) {
            if (warp.name.equalsIgnoreCase(oldName)) {
                warp.name = newName;
                savePlayerWarps(player.getUUID());
                player.sendSystemMessage(Component.literal("§a✦ Warp renamed to '" + newName + "'!"));
                return true;
            }
        }
        
        player.sendSystemMessage(Component.literal("§cNo warp found with that name!"));
        return false;
    }
    
    /**
     * Teleport player directly to a warp by name
     */
    public static boolean teleportToWarp(ServerPlayer player, String name) {
        List<WarpPoint> warps = getWarps(player);
        
        for (WarpPoint warp : warps) {
            if (warp.name.equalsIgnoreCase(name)) {
                return teleportToWarpPoint(player, warp);
            }
        }
        
        player.sendSystemMessage(Component.literal("§cNo warp found with that name!"));
        return false;
    }
    
    /**
     * Teleport player to a specific warp point
     */
    public static boolean teleportToWarpPoint(ServerPlayer player, WarpPoint warp) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) return false;
        
        // Find the dimension by iterating through all levels
        ServerLevel targetWorld = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().toString().equals(warp.dimension)) {
                targetWorld = level;
                break;
            }
        }
        
        if (targetWorld == null) {
            player.sendSystemMessage(Component.literal("§cCannot find dimension: " + warp.dimension));
            return false;
        }
        
        // Teleport
        player.teleportTo(
            targetWorld,
            warp.x,
            warp.y,
            warp.z,
            Set.of(),
            warp.yaw,
            warp.pitch,
            false
        );
        
        player.sendSystemMessage(Component.literal("§a✦ Warped to '" + warp.name + "'!"));
        return true;
    }
    
    /**
     * Teleport player to the Warp Hub dimension
     */
    public static boolean teleportToWarpHub(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) return false;
        
        ServerLevel warpHub = server.getLevel(WARP_HUB);
        if (warpHub == null) {
            player.sendSystemMessage(Component.literal("§cWarp Hub dimension not found! Make sure the mod is properly installed."));
            return false;
        }
        
        // Save current location before teleporting
        savePreviousLocation(player);
        
        // Calculate spawn position in warp hub
        // Center platform at y=64
        double x = 0.5;
        double y = 65.0;
        double z = 0.5;
        
        // Build the central platform if it doesn't exist
        buildWarpHubPlatform(warpHub, player);
        
        // Teleport
        player.teleportTo(
            warpHub,
            x,
            y,
            z,
            Set.of(),
            0.0f,
            0.0f,
            false
        );
        
        player.sendSystemMessage(Component.literal("§d✦ Welcome to the Warp Hub!"));
        player.sendSystemMessage(Component.literal("§7Walk through a portal to teleport to that warp."));
        player.sendSystemMessage(Component.literal("§7The §e'Previous Location'§7 portal returns you where you came from."));
        
        return true;
    }
    
    /**
     * Build the warp hub platform and portals for a player
     */
    public static void buildWarpHubPlatform(ServerLevel warpHub, ServerPlayer player) {
        List<WarpPoint> warps = getWarps(player);
        int platformY = 64;
        
        // Clear old structures and build central platform
        // Platform is 21x21 blocks centered at 0,0
        int platformRadius = 10;
        
        for (int xPos = -platformRadius; xPos <= platformRadius; xPos++) {
            for (int zPos = -platformRadius; zPos <= platformRadius; zPos++) {
                BlockPos pos = new BlockPos(xPos, platformY, zPos);
                
                // Clear above
                for (int yOffset = 1; yOffset <= 10; yOffset++) {
                    warpHub.setBlock(pos.above(yOffset), Blocks.AIR.defaultBlockState(), 2);
                }
                
                // Build floor
                if (Math.abs(xPos) <= 2 && Math.abs(zPos) <= 2) {
                    // Center is gold blocks
                    warpHub.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
                } else if ((Math.abs(xPos) + Math.abs(zPos)) % 2 == 0) {
                    warpHub.setBlock(pos, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
                } else {
                    warpHub.setBlock(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                }
            }
        }
        
        // Build portal pillars for each warp
        int totalWarps = warps.size() + 1; // +1 for "Previous Location"
        double radius = 6.0;
        
        // "Previous Location" portal first
        double angle = 0;
        int px = (int) Math.round(radius * Math.cos(angle));
        int pz = (int) Math.round(radius * Math.sin(angle));
        buildPortalPillar(warpHub, new BlockPos(px, platformY + 1, pz), "Previous Location", 0xFFFF55);
        
        // Player warps
        for (int i = 0; i < warps.size(); i++) {
            WarpPoint warp = warps.get(i);
            angle = (2 * Math.PI * (i + 1)) / Math.max(totalWarps, 8);
            px = (int) Math.round(radius * Math.cos(angle));
            pz = (int) Math.round(radius * Math.sin(angle));
            buildPortalPillar(warpHub, new BlockPos(px, platformY + 1, pz), warp.name, 0x55FFFF);
        }
        
        // Glowstone lighting
        warpHub.setBlock(new BlockPos(0, platformY + 5, 0), Blocks.GLOWSTONE.defaultBlockState(), 2);
    }
    
    /**
     * Build a portal pillar at a position
     */
    private static void buildPortalPillar(ServerLevel world, BlockPos base, String name, int color) {
        // Pillar base
        world.setBlock(base, Blocks.OBSIDIAN.defaultBlockState(), 2);
        world.setBlock(base.above(), Blocks.OBSIDIAN.defaultBlockState(), 2);
        
        // Portal block (end gateway for effect)
        world.setBlock(base.above(2), Blocks.END_GATEWAY.defaultBlockState(), 2);
        
        // Top decoration
        world.setBlock(base.above(3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
    }
    
    /**
     * Save player's location before entering warp hub
     */
    private static void savePreviousLocation(ServerPlayer player) {
        PreviousLocation prev = new PreviousLocation(
            player.level().dimension().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot()
        );
        PREVIOUS_LOCATIONS.put(player.getUUID(), prev);
    }
    
    /**
     * Return player to their previous location
     */
    public static boolean returnToPreviousLocation(ServerPlayer player) {
        PreviousLocation prev = PREVIOUS_LOCATIONS.get(player.getUUID());
        if (prev == null) {
            player.sendSystemMessage(Component.literal("§cNo previous location saved!"));
            return false;
        }
        
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) return false;
        
        // Find the dimension by iterating through all levels
        ServerLevel targetWorld = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().toString().equals(prev.dimension)) {
                targetWorld = level;
                break;
            }
        }
        
        if (targetWorld == null) {
            player.sendSystemMessage(Component.literal("§cCannot find dimension: " + prev.dimension));
            return false;
        }
        
        player.teleportTo(
            targetWorld,
            prev.x,
            prev.y,
            prev.z,
            Set.of(),
            prev.yaw,
            prev.pitch,
            false
        );
        
        PREVIOUS_LOCATIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("§a✦ Returned to previous location!"));
        return true;
    }
    
    /**
     * Check if player is in the Warp Hub
     */
    public static boolean isInWarpHub(ServerPlayer player) {
        return player.level().dimension().equals(WARP_HUB);
    }
    
    // ============= PERSISTENCE =============
    
    private static void savePlayerWarps(UUID uuid) {
        if (DATA_DIR == null) return;
        
        List<WarpPoint> warps = PLAYER_WARPS.get(uuid);
        if (warps == null) return;
        
        Path file = DATA_DIR.resolve(uuid + ".dat");
        CompoundTag root = new CompoundTag();
        ListTag warpList = new ListTag();
        
        for (WarpPoint warp : warps) {
            CompoundTag warpTag = new CompoundTag();
            warpTag.putString("Name", warp.name);
            warpTag.putString("Dimension", warp.dimension);
            warpTag.putDouble("X", warp.x);
            warpTag.putDouble("Y", warp.y);
            warpTag.putDouble("Z", warp.z);
            warpTag.putFloat("Yaw", warp.yaw);
            warpTag.putFloat("Pitch", warp.pitch);
            warpList.add(warpTag);
        }
        
        root.put("Warps", warpList);
        
        try {
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            AioMod.LOGGER.error("Failed to save warps for " + uuid, e);
        }
    }
    
    private static void loadPlayerWarps(UUID uuid) {
        if (DATA_DIR == null) return;
        
        Path file = DATA_DIR.resolve(uuid + ".dat");
        if (!Files.exists(file)) return;
        
        try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            ListTag warpList = root.getListOrEmpty("Warps");
            List<WarpPoint> warps = new ArrayList<>();
            
            for (int i = 0; i < warpList.size(); i++) {
                CompoundTag warpTag = warpList.getCompoundOrEmpty(i);
                WarpPoint warp = new WarpPoint(
                    warpTag.getStringOr("Name", "Unnamed"),
                    warpTag.getStringOr("Dimension", "minecraft:overworld"),
                    warpTag.getDoubleOr("X", 0),
                    warpTag.getDoubleOr("Y", 64),
                    warpTag.getDoubleOr("Z", 0),
                    warpTag.getFloatOr("Yaw", 0),
                    warpTag.getFloatOr("Pitch", 0)
                );
                warps.add(warp);
            }
            
            PLAYER_WARPS.put(uuid, warps);
        } catch (IOException e) {
            AioMod.LOGGER.error("Failed to load warps for " + uuid, e);
        }
    }
    
    private static void saveAllWarps() {
        for (UUID uuid : PLAYER_WARPS.keySet()) {
            savePlayerWarps(uuid);
        }
        AioMod.LOGGER.info("All player warps saved.");
    }
    
    private static void loadAllWarps() {
        if (DATA_DIR == null) return;
        
        try {
            if (Files.exists(DATA_DIR)) {
                Files.list(DATA_DIR)
                    .filter(p -> p.toString().endsWith(".dat"))
                    .forEach(p -> {
                        String filename = p.getFileName().toString();
                        String uuidStr = filename.substring(0, filename.length() - 4);
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            loadPlayerWarps(uuid);
                        } catch (IllegalArgumentException e) {
                            // Not a valid UUID file, skip
                        }
                    });
            }
        } catch (IOException e) {
            AioMod.LOGGER.error("Failed to load warps", e);
        }
    }
    
    // ============= DATA CLASSES =============
    
    public static class WarpPoint {
        public String name;
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;
        
        public WarpPoint(String name, String dimension, double x, double y, double z, float yaw, float pitch) {
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
    
    public static class PreviousLocation {
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;
        
        public PreviousLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
