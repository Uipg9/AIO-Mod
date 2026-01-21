package com.baesp.aio.config;

import com.baesp.aio.AioMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AioConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aio-mod.json");
    
    // === ASCENDANCY CONFIG ===
    public boolean ascendancyEnabled = true;
    public int soulXpPerKill = 10;
    public int soulXpPerOreBreak = 5;
    public int prestigePointsPerAscension = 1;
    public double ascensionXpMultiplier = 1.5;
    
    // === RPG/SKILLS CONFIG ===
    public boolean skillsEnabled = true;
    public int maxSkillLevel = 10;
    public double skillBonusPerLevel = 0.05; // 5% per level
    public int xpPerSkillAction = 10;
    
    // === ECONOMY CONFIG ===
    public boolean economyEnabled = true;
    public long startingMoney = 100;
    public int smeltingRewardCoins = 1;
    
    // === FAST SMELT CONFIG ===
    public boolean fastSmeltEnabled = true;
    public int furnaceCookTime = 1; // 1 tick
    public int blastFurnaceCookTime = 1;
    public int smokerCookTime = 1;
    public int campfireCookTime = 1;
    
    // === 1-TICK HOPPERS CONFIG ===
    public boolean fastHoppersEnabled = true;
    public int hopperCooldown = 1; // 1 tick (vanilla is 8)
    
    // === VILLAGE SPAWN POINT CONFIG ===
    public boolean villageSpawnEnabled = true;
    public String villageLocateTag = "#minecraft:village";
    public String villageSpawnStructureTag = "minecraft:village";
    public int villageSpawnSearchRadius = 10000;
    public boolean fallbackToDefaultTag = true;
    
    // === SQUAT GROW CONFIG ===
    public boolean squatGrowEnabled = true;
    public int squatGrowRange = 3;
    public float squatGrowChance = 0.5f;
    public int squatGrowMultiplier = 4;
    public boolean requireHoeForSquatGrow = false;
    
    // === SLEEP WARP CONFIG ===
    public boolean sleepWarpEnabled = true;
    public int sleepWarpMaxTicksAdded = 40;
    public double sleepWarpPlayerMultiplier = 0.6;
    public boolean sleepWarpTickBlockEntities = true;
    public boolean sleepWarpTickRandomBlocks = true;
    public boolean sleepWarpTickSnow = true;
    public boolean sleepWarpTickIce = true;
    public boolean sleepWarpTickLightning = true;
    
    // === HUD CONFIG ===
    public boolean hudEnabled = true;
    public int hudX = 5;
    public int hudY = 5;
    
    public static AioConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                AioConfig config = GSON.fromJson(json, AioConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                AioMod.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        
        AioConfig config = new AioConfig();
        config.save();
        return config;
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            AioMod.LOGGER.error("Failed to save config", e);
        }
    }
}
