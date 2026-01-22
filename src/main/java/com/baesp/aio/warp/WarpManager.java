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
                } else if (portalIndex == -2) {
                    // Home Dimension portal
                    HomeManager.teleportToHome(player);
                } else if (portalIndex == -3) {
                    // Overworld Spawn portal
                    teleportToOverworldSpawn(player);
                } else {
                    // Player warp portal
                    List<WarpPoint> warps = getWarps(player);
                    if (portalIndex >= 0 && portalIndex < warps.size()) {
                        teleportToWarpPoint(player, warps.get(portalIndex));
                    }
                }
                return;
            }
        }
    }
    
    /**
     * Teleport player to their overworld spawn point
     * Uses ascension spawn if they've ascended, otherwise world spawn
     */
    private static void teleportToOverworldSpawn(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server == null) return;
        
        ServerLevel overworld = server.overworld();
        var playerData = com.baesp.aio.data.PlayerDataManager.getData(player);
        
        double spawnX, spawnY, spawnZ;
        
        // Check if player has an ascension spawn point
        if (playerData.ascendancy.ascensionCount > 0 && !Double.isNaN(playerData.ascendancy.ascensionSpawnX)) {
            spawnX = playerData.ascendancy.ascensionSpawnX;
            spawnY = playerData.ascendancy.ascensionSpawnY;
            spawnZ = playerData.ascendancy.ascensionSpawnZ;
            player.sendSystemMessage(Component.literal("§a✦ Warped to your Ascension Spawn!"));
        } else {
            // Use cached village spawn or default position
            BlockPos villageSpawn = com.baesp.aio.villagespawn.VillageSpawnManager.getPlayerVillageSpawn(player.getUUID());
            if (villageSpawn != null) {
                spawnX = villageSpawn.getX() + 0.5;
                spawnY = villageSpawn.getY();
                spawnZ = villageSpawn.getZ() + 0.5;
            } else {
                // Default spawn at 0,64,0
                spawnX = 0.5;
                spawnY = 64;
                spawnZ = 0.5;
            }
            player.sendSystemMessage(Component.literal("§a✦ Warped to World Spawn!"));
        }
        
        player.teleportTo(
            overworld,
            spawnX,
            spawnY,
            spawnZ,
            Set.of(),
            0f,
            0f,
            false
        );
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
        MinecraftServer server = warpHub.getServer();
        
        // Clear portal positions for this player
        PORTAL_POSITIONS.put(uuid, new HashMap<>());
        Map<BlockPos, Integer> portalMap = PORTAL_POSITIONS.get(uuid);
        
        // Remove old armor stands in the area
        AABB clearArea = new AABB(-25, PLATFORM_Y - 5, -25, 25, PLATFORM_Y + 20, 25);
        List<ArmorStand> oldStands = warpHub.getEntitiesOfClass(ArmorStand.class, clearArea);
        for (ArmorStand stand : oldStands) {
            stand.discard();
        }
        
        // Build beautiful main platform
        buildMainPlatform(warpHub);
        
        // Build decorative structures
        buildDecorativeStructures(warpHub);
        
        // === AUTO WARPS (Special portals that are always present) ===
        // Index -1 = Previous Location
        // Index -2 = Home Dimension
        // Index -3 = Overworld Spawn
        
        // Portal positions - arranged in a beautiful pattern
        // Inner ring (radius 6): Auto warps
        // Outer ring (radius 10): Player warps
        
        // Previous Location (East, Yellow)
        BlockPos prevPos = new BlockPos(6, PLATFORM_Y, 0);
        buildPortalPad(warpHub, prevPos, "§e⟲ Previous Location", PortalType.PREVIOUS);
        portalMap.put(prevPos, -1);
        
        // Home Dimension (West, Green)
        BlockPos homePos = new BlockPos(-6, PLATFORM_Y, 0);
        buildPortalPad(warpHub, homePos, "§a🏠 Home Dimension", PortalType.HOME);
        portalMap.put(homePos, -2);
        
        // Overworld Spawn (South, Blue) - Gets player's current spawn point
        BlockPos overworldPos = new BlockPos(0, PLATFORM_Y, 6);
        String overworldName = getOverworldSpawnName(player, server);
        buildPortalPad(warpHub, overworldPos, "§b🌍 " + overworldName, PortalType.OVERWORLD);
        portalMap.put(overworldPos, -3);
        
        // Player warps in outer ring
        double outerRadius = 10.0;
        int startAngle = 45; // Start at NE to avoid auto-warp positions
        for (int i = 0; i < warps.size(); i++) {
            WarpPoint warp = warps.get(i);
            double angle = Math.toRadians(startAngle + (i * 45)); // 45 degree spacing
            if (i >= 8) {
                // Second ring for more warps
                outerRadius = 14.0;
                angle = Math.toRadians((i - 8) * 45 + 22.5);
            }
            int px = (int) Math.round(outerRadius * Math.cos(angle));
            int pz = (int) Math.round(outerRadius * Math.sin(angle));
            BlockPos warpPos = new BlockPos(px, PLATFORM_Y, pz);
            buildPortalPad(warpHub, warpPos, "§d✦ " + warp.name, PortalType.PLAYER);
            portalMap.put(warpPos, i);
        }
    }
    
    /**
     * Get the name for the Overworld spawn portal
     */
    private static String getOverworldSpawnName(ServerPlayer player, MinecraftServer server) {
        var playerData = com.baesp.aio.data.PlayerDataManager.getData(player);
        if (playerData.ascendancy.ascensionCount > 0 && !Double.isNaN(playerData.ascendancy.ascensionSpawnX)) {
            return "Ascension Spawn";
        }
        return "World Spawn";
    }
    
    /**
     * Build the main platform with beautiful design
     */
    private static void buildMainPlatform(ServerLevel world) {
        // Extended platform with gradient design
        int mainRadius = 18;
        
        for (int x = -mainRadius; x <= mainRadius; x++) {
            for (int z = -mainRadius; z <= mainRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                BlockPos pos = new BlockPos(x, PLATFORM_Y, z);
                
                // Clear above
                for (int y = 1; y <= 15; y++) {
                    world.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 2);
                }
                
                // Circular platform with gradient
                if (dist <= mainRadius) {
                    if (dist <= 3) {
                        // Center - Crystal design
                        world.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
                    } else if (dist <= 5) {
                        // Inner ring - Purple
                        world.setBlock(pos, Blocks.PURPUR_BLOCK.defaultBlockState(), 2);
                    } else if (dist <= 8) {
                        // Middle ring - Checkered
                        if ((Math.abs(x) + Math.abs(z)) % 2 == 0) {
                            world.setBlock(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                        } else {
                            world.setBlock(pos, Blocks.DEEPSLATE_TILES.defaultBlockState(), 2);
                        }
                    } else if (dist <= 12) {
                        // Outer area - Darker
                        world.setBlock(pos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                    } else {
                        // Edge - Blackstone
                        world.setBlock(pos, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        // Platform edge border
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            int x = (int) Math.round(mainRadius * Math.cos(rad));
            int z = (int) Math.round(mainRadius * Math.sin(rad));
            world.setBlock(new BlockPos(x, PLATFORM_Y, z), Blocks.GILDED_BLACKSTONE.defaultBlockState(), 2);
        }
    }
    
    /**
     * Build decorative structures around the hub
     */
    private static void buildDecorativeStructures(ServerLevel world) {
        // Center beacon-like structure
        buildCenterBeacon(world);
        
        // Corner pillars with crystals
        int pillarDist = 15;
        buildCornerPillar(world, new BlockPos(pillarDist, PLATFORM_Y, pillarDist));
        buildCornerPillar(world, new BlockPos(-pillarDist, PLATFORM_Y, pillarDist));
        buildCornerPillar(world, new BlockPos(pillarDist, PLATFORM_Y, -pillarDist));
        buildCornerPillar(world, new BlockPos(-pillarDist, PLATFORM_Y, -pillarDist));
        
        // Arches between pillars
        buildArch(world, pillarDist, 0); // East
        buildArch(world, -pillarDist, 0); // West
        buildArch(world, 0, pillarDist); // South
        buildArch(world, 0, -pillarDist); // North
        
        // Floating crystal rings
        buildFloatingRing(world, 10, PLATFORM_Y + 8);
        buildFloatingRing(world, 6, PLATFORM_Y + 12);
    }
    
    /**
     * Build the center beacon structure
     */
    private static void buildCenterBeacon(ServerLevel world) {
        // Base
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(new BlockPos(x, PLATFORM_Y + 1, z), Blocks.QUARTZ_BLOCK.defaultBlockState(), 2);
            }
        }
        
        // Pillar
        for (int y = 2; y <= 5; y++) {
            world.setBlock(new BlockPos(0, PLATFORM_Y + y, 0), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
        }
        
        // Top
        world.setBlock(new BlockPos(0, PLATFORM_Y + 6, 0), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(new BlockPos(0, PLATFORM_Y + 7, 0), Blocks.BEACON.defaultBlockState(), 2);
        
        // Surrounding end rods
        world.setBlock(new BlockPos(1, PLATFORM_Y + 3, 0), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(new BlockPos(-1, PLATFORM_Y + 3, 0), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(new BlockPos(0, PLATFORM_Y + 3, 1), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(new BlockPos(0, PLATFORM_Y + 3, -1), Blocks.END_ROD.defaultBlockState(), 2);
    }
    
    /**
     * Build a corner pillar with crystal top
     */
    private static void buildCornerPillar(ServerLevel world, BlockPos base) {
        // Base
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(base.offset(x, 0, z), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
            }
        }
        
        // Pillar
        for (int y = 1; y <= 6; y++) {
            world.setBlock(base.above(y), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
        }
        
        // Crystal top
        world.setBlock(base.above(7), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
        world.setBlock(base.above(8), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2);
        
        // Lanterns
        world.setBlock(base.offset(1, 4, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        world.setBlock(base.offset(-1, 4, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        world.setBlock(base.offset(0, 4, 1), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        world.setBlock(base.offset(0, 4, -1), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
    }
    
    /**
     * Build an arch structure
     */
    private static void buildArch(ServerLevel world, int x, int z) {
        // Arch pillars
        for (int y = 1; y <= 5; y++) {
            if (x != 0) {
                world.setBlock(new BlockPos(x, PLATFORM_Y + y, z - 2), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
                world.setBlock(new BlockPos(x, PLATFORM_Y + y, z + 2), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            } else {
                world.setBlock(new BlockPos(x - 2, PLATFORM_Y + y, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
                world.setBlock(new BlockPos(x + 2, PLATFORM_Y + y, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            }
        }
        
        // Arch top
        if (x != 0) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlock(new BlockPos(x, PLATFORM_Y + 6, z + dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
            }
        } else {
            for (int dx = -2; dx <= 2; dx++) {
                world.setBlock(new BlockPos(x + dx, PLATFORM_Y + 6, z), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
            }
        }
    }
    
    /**
     * Build a floating ring of crystals
     */
    private static void buildFloatingRing(ServerLevel world, int radius, int y) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            int x = (int) Math.round(radius * Math.cos(angle));
            int z = (int) Math.round(radius * Math.sin(angle));
            world.setBlock(new BlockPos(x, y, z), Blocks.END_ROD.defaultBlockState(), 2);
        }
    }
    
    // Portal types for visual distinction
    private enum PortalType {
        PREVIOUS, HOME, OVERWORLD, PLAYER
    }
    
    /**
     * Build a portal pad at floor level with floating name
     */
    private static void buildPortalPad(ServerLevel world, BlockPos center, String name, PortalType type) {
        // Get blocks based on portal type
        net.minecraft.world.level.block.state.BlockState centerBlock;
        net.minecraft.world.level.block.state.BlockState surroundBlock;
        
        switch (type) {
            case PREVIOUS:
                centerBlock = Blocks.LODESTONE.defaultBlockState();
                surroundBlock = Blocks.GOLD_BLOCK.defaultBlockState();
                break;
            case HOME:
                centerBlock = Blocks.MOSS_BLOCK.defaultBlockState();
                surroundBlock = Blocks.EMERALD_BLOCK.defaultBlockState();
                break;
            case OVERWORLD:
                centerBlock = Blocks.GRASS_BLOCK.defaultBlockState();
                surroundBlock = Blocks.LAPIS_BLOCK.defaultBlockState();
                break;
            case PLAYER:
            default:
                centerBlock = Blocks.END_PORTAL_FRAME.defaultBlockState();
                surroundBlock = Blocks.DIAMOND_BLOCK.defaultBlockState();
                break;
        }
        
        // Build 3x3 portal pad on the floor
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (dx == 0 && dz == 0) {
                    world.setBlock(pos, centerBlock, 2);
                } else {
                    world.setBlock(pos, surroundBlock, 2);
                }
            }
        }
        
        // Corner decorations
        world.setBlock(center.offset(-1, 1, -1), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(center.offset(1, 1, -1), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(center.offset(-1, 1, 1), Blocks.END_ROD.defaultBlockState(), 2);
        world.setBlock(center.offset(1, 1, 1), Blocks.END_ROD.defaultBlockState(), 2);
        
        // Spawn armor stand with name floating above
        ArmorStand nameTag = new ArmorStand(EntityType.ARMOR_STAND, world);
        nameTag.setPos(center.getX() + 0.5, center.getY() + 2.5, center.getZ() + 0.5);
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
