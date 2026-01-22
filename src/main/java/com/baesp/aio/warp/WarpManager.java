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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
 * - Warp Hub dimension with portal pads on the ground
 * - Walk into portal to teleport - no climbing needed
 * - Floating name tags above each portal
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
    
    // Portal positions for collision detection: stores which portal is at each position
    // Map of player UUID -> Map of BlockPos -> portal index (-1 = previous location)
    private static final Map<UUID, Map<BlockPos, Integer>> PORTAL_POSITIONS = new ConcurrentHashMap<>();
    
    // Cooldown to prevent teleport spam
    private static final Map<UUID, Long> TELEPORT_COOLDOWN = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000; // 1 second cooldown
    
    private static Path DATA_DIR;
    private static final int PLATFORM_Y = 64;
    private static final double PORTAL_RADIUS = 6.0;
    
    public static void init() {
        // Register tick event for portal collision detection
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isInWarpHub(player)) {
                    checkPortalCollision(player);
                }
            }
        });
        
        AioMod.LOGGER.info("Warp Manager initialized.");
    }
    
    /**
     * Check if player is standing on a portal and teleport them
     */
    private static void checkPortalCollision(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<BlockPos, Integer> portals = PORTAL_POSITIONS.get(uuid);
        if (portals == null || portals.isEmpty()) return;
        
        // Check cooldown
        Long lastTeleport = TELEPORT_COOLDOWN.get(uuid);
        if (lastTeleport != null && System.currentTimeMillis() - lastTeleport < COOLDOWN_MS) {
            return;
        }
        
        // Get player's feet position
        BlockPos playerPos = player.blockPosition();
        
        // Check if player is on any portal pad (check the block they're standing on)
        for (Map.Entry<BlockPos, Integer> entry : portals.entrySet()) {
            BlockPos portalPos = entry.getKey();
            // Check if player is within the 3x3 portal area at floor level
            if (Math.abs(playerPos.getX() - portalPos.getX()) <= 1 &&
                Math.abs(playerPos.getZ() - portalPos.getZ()) <= 1 &&
                playerPos.getY() == PLATFORM_Y + 1) {
                
                int portalIndex = entry.getValue();
                TELEPORT_COOLDOWN.put(uuid, System.currentTimeMillis());
                
                if (portalIndex == -1) {
                    // Previous location portal
                    returnToPreviousLocation(player);
                } else {
                    // Warp portal
                    List<WarpPoint> warps = getWarps(player);
                    if (portalIndex >= 0 && portalIndex < warps.size()) {
                        teleportToWarpPoint(player, warps.get(portalIndex));
                    }
                }
                return;
            }
        }
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
        
        // Create the warp
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
        
        // Build the platform and portals
        buildWarpHubPlatform(warpHub, player);
        
        // Teleport to center
        player.teleportTo(
            warpHub,
            0.5,
            PLATFORM_Y + 1.0,
            0.5,
            Set.of(),
            0.0f,
            0.0f,
            false
        );
        
        player.sendSystemMessage(Component.literal("§d✦ Welcome to the Warp Hub!"));
        player.sendSystemMessage(Component.literal("§7Step onto a portal pad to teleport."));
        player.sendSystemMessage(Component.literal("§7The §e⟲ Previous Location§7 pad returns you where you came from."));
        
        return true;
    }
    
    /**
     * Build the warp hub platform and portals for a player
     */
    public static void buildWarpHubPlatform(ServerLevel warpHub, ServerPlayer player) {
        List<WarpPoint> warps = getWarps(player);
        UUID uuid = player.getUUID();
        
        // Clear portal positions for this player
        PORTAL_POSITIONS.put(uuid, new HashMap<>());
        Map<BlockPos, Integer> portalMap = PORTAL_POSITIONS.get(uuid);
        
        // Remove old armor stands in the area
        AABB clearArea = new AABB(-15, PLATFORM_Y, -15, 15, PLATFORM_Y + 10, 15);
        List<ArmorStand> oldStands = warpHub.getEntitiesOfClass(ArmorStand.class, clearArea);
        for (ArmorStand stand : oldStands) {
            stand.discard();
        }
        
        // Clear old structures and build central platform
        int platformRadius = 10;
        
        for (int xPos = -platformRadius; xPos <= platformRadius; xPos++) {
            for (int zPos = -platformRadius; zPos <= platformRadius; zPos++) {
                BlockPos pos = new BlockPos(xPos, PLATFORM_Y, zPos);
                
                // Clear above
                for (int yOffset = 1; yOffset <= 10; yOffset++) {
                    warpHub.setBlock(pos.above(yOffset), Blocks.AIR.defaultBlockState(), 2);
                }
                
                // Build floor - checkered pattern
                if (Math.abs(xPos) <= 2 && Math.abs(zPos) <= 2) {
                    // Center spawn area - crying obsidian for visual
                    warpHub.setBlock(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
                } else if ((Math.abs(xPos) + Math.abs(zPos)) % 2 == 0) {
                    warpHub.setBlock(pos, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
                } else {
                    warpHub.setBlock(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                }
            }
        }
        
        // Build portal pads in a circle
        int totalWarps = warps.size() + 1; // +1 for "Previous Location"
        
        // "Previous Location" portal first (at angle 0 = east)
        double angle = 0;
        int px = (int) Math.round(PORTAL_RADIUS * Math.cos(angle));
        int pz = (int) Math.round(PORTAL_RADIUS * Math.sin(angle));
        buildPortalPad(warpHub, new BlockPos(px, PLATFORM_Y, pz), "§e⟲ Previous Location", true);
        portalMap.put(new BlockPos(px, PLATFORM_Y, pz), -1);
        
        // Player warps
        for (int i = 0; i < warps.size(); i++) {
            WarpPoint warp = warps.get(i);
            angle = (2 * Math.PI * (i + 1)) / Math.max(totalWarps, 8);
            px = (int) Math.round(PORTAL_RADIUS * Math.cos(angle));
            pz = (int) Math.round(PORTAL_RADIUS * Math.sin(angle));
            buildPortalPad(warpHub, new BlockPos(px, PLATFORM_Y, pz), "§b" + warp.name, false);
            portalMap.put(new BlockPos(px, PLATFORM_Y, pz), i);
        }
        
        // Center glowstone for lighting (floating above)
        warpHub.setBlock(new BlockPos(0, PLATFORM_Y + 6, 0), Blocks.GLOWSTONE.defaultBlockState(), 2);
        warpHub.setBlock(new BlockPos(3, PLATFORM_Y + 4, 3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        warpHub.setBlock(new BlockPos(-3, PLATFORM_Y + 4, 3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        warpHub.setBlock(new BlockPos(3, PLATFORM_Y + 4, -3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        warpHub.setBlock(new BlockPos(-3, PLATFORM_Y + 4, -3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
    }
    
    /**
     * Build a portal pad at floor level with floating name
     */
    private static void buildPortalPad(ServerLevel world, BlockPos center, String name, boolean isPreviousLocation) {
        // Build 3x3 portal pad on the floor
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (dx == 0 && dz == 0) {
                    // Center block - end portal frame for the glowing effect
                    world.setBlock(pos, Blocks.END_PORTAL_FRAME.defaultBlockState(), 2);
                } else {
                    // Surrounding blocks - purple for previous, cyan for warps
                    if (isPreviousLocation) {
                        world.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
                    } else {
                        world.setBlock(pos, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        // Spawn armor stand with name floating above
        ArmorStand nameTag = new ArmorStand(EntityType.ARMOR_STAND, world);
        nameTag.setPos(center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 0.5);
        nameTag.setCustomName(Component.literal(name));
        nameTag.setCustomNameVisible(true);
        nameTag.setInvisible(true);
        nameTag.setNoGravity(true);
        nameTag.setInvulnerable(true);
        world.addFreshEntity(nameTag);
    }
    
    /**
     * Save player's location before entering warp hub
     */
    private static void savePreviousLocation(ServerPlayer player) {
        // Don't save if already in warp hub
        if (isInWarpHub(player)) return;
        
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
