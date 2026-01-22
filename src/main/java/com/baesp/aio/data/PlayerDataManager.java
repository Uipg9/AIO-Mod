package com.baesp.aio.data;

import com.baesp.aio.AioMod;
import com.baesp.aio.ascendancy.AscendancyData;
import com.baesp.aio.rpg.SkillsData;
import com.baesp.aio.rpg.economy.EconomyData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Map<UUID, PlayerData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static Path DATA_DIR;
    private static MinecraftServer server;
    
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            DATA_DIR = FabricLoader.getInstance().getGameDir()
                .resolve("world")
                .resolve("aio_data")
                .resolve("players");
            try {
                Files.createDirectories(DATA_DIR);
            } catch (IOException e) {
                AioMod.LOGGER.error("Failed to create data directory", e);
            }
        });
    }
    
    public static PlayerData getData(ServerPlayer player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), uuid -> new PlayerData());
    }
    
    public static PlayerData getData(UUID uuid) {
        return PLAYER_DATA.computeIfAbsent(uuid, u -> new PlayerData());
    }
    
    public static void loadPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Path file = DATA_DIR.resolve(uuid + ".dat");
        
        if (Files.exists(file)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                PlayerData data = new PlayerData();
                data.load(tag);
                PLAYER_DATA.put(uuid, data);
                AioMod.LOGGER.debug("Loaded data for player {}", player.getName().getString());
            } catch (IOException e) {
                AioMod.LOGGER.error("Failed to load data for player " + uuid, e);
                PLAYER_DATA.put(uuid, new PlayerData());
            }
        } else {
            PLAYER_DATA.put(uuid, new PlayerData());
        }
    }
    
    public static void savePlayer(ServerPlayer player) {
        savePlayer(player.getUUID());
    }
    
    public static void savePlayer(UUID uuid) {
        PlayerData data = PLAYER_DATA.get(uuid);
        if (data == null || DATA_DIR == null) return;
        
        Path file = DATA_DIR.resolve(uuid + ".dat");
        try {
            CompoundTag tag = new CompoundTag();
            data.save(tag);
            NbtIo.writeCompressed(tag, file);
        } catch (IOException e) {
            AioMod.LOGGER.error("Failed to save data for player " + uuid, e);
        }
    }
    
    public static void saveAllPlayers() {
        for (UUID uuid : PLAYER_DATA.keySet()) {
            savePlayer(uuid);
        }
    }
    
    // === Helper methods for storing arbitrary boolean flags ===
    private static final Map<UUID, Map<String, Boolean>> CUSTOM_BOOLEANS = new ConcurrentHashMap<>();
    
    public static boolean getBoolean(ServerPlayer player, String key) {
        Map<String, Boolean> playerBooleans = CUSTOM_BOOLEANS.get(player.getUUID());
        if (playerBooleans == null) return false;
        return playerBooleans.getOrDefault(key, false);
    }
    
    public static void setBoolean(ServerPlayer player, String key, boolean value) {
        CUSTOM_BOOLEANS.computeIfAbsent(player.getUUID(), uuid -> new ConcurrentHashMap<>())
            .put(key, value);
    }
    
    public static class PlayerData {
        // Ascendancy
        public AscendancyData ascendancy = new AscendancyData();
        
        // RPG Skills
        public SkillsData skills = new SkillsData();
        
        // Economy
        public EconomyData economy = new EconomyData();
        
        // Squat Grow toggle
        public boolean squatGrowEnabled = false;
        
        // Starter Kit received flag
        public boolean receivedStarterKit = false;
        
        public void load(CompoundTag tag) {
            if (tag.contains("Ascendancy")) {
                ascendancy.load(tag.getCompoundOrEmpty("Ascendancy"));
            }
            if (tag.contains("Skills")) {
                skills.load(tag.getCompoundOrEmpty("Skills"));
            }
            if (tag.contains("Economy")) {
                economy.load(tag.getCompoundOrEmpty("Economy"));
            }
            squatGrowEnabled = tag.getBooleanOr("SquatGrowEnabled", false);
            receivedStarterKit = tag.getBooleanOr("ReceivedStarterKit", false);
        }
        
        public void save(CompoundTag tag) {
            CompoundTag ascendancyTag = new CompoundTag();
            ascendancy.save(ascendancyTag);
            tag.put("Ascendancy", ascendancyTag);
            
            CompoundTag skillsTag = new CompoundTag();
            skills.save(skillsTag);
            tag.put("Skills", skillsTag);
            
            CompoundTag economyTag = new CompoundTag();
            economy.save(economyTag);
            tag.put("Economy", economyTag);
            
            tag.putBoolean("SquatGrowEnabled", squatGrowEnabled);
            tag.putBoolean("ReceivedStarterKit", receivedStarterKit);
        }
    }
}
