package com.baesp.aio;

import com.baesp.aio.ascendancy.AscendancyManager;
import com.baesp.aio.commands.AioCommands;
import com.baesp.aio.config.AioConfig;
import com.baesp.aio.data.PlayerDataManager;
import com.baesp.aio.features.*;
import com.baesp.aio.network.AioNetwork;
import com.baesp.aio.rpg.SkillsManager;
import com.baesp.aio.rpg.economy.EconomyManager;
import com.baesp.aio.rpg.economy.ShopManager;
import com.baesp.aio.sleepwarp.SleepWarpManager;
import com.baesp.aio.squat.SquatGrowManager;
import com.baesp.aio.villagespawn.VillageSpawnManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AioMod implements ModInitializer {
    public static final String MOD_ID = "aio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public static AioConfig CONFIG;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing All-in-One Mod for Minecraft 1.21.11");
        
        // Load config
        CONFIG = AioConfig.load();
        
        // Register networking
        AioNetwork.registerServer();
        
        // Initialize core managers
        PlayerDataManager.init();
        AscendancyManager.init();
        SkillsManager.init();
        EconomyManager.init();
        ShopManager.register();
        SquatGrowManager.init();
        SleepWarpManager.init();
        VillageSpawnManager.init();
        
        // Initialize new feature managers
        VoidMagnetManager.register();
        VeinMiningManager.register();
        StarterKitManager.register();
        AutoReplantManager.register();
        SleepSoonerManager.register();
        DeathSafetyManager.register();
        InfiniteTradingManager.register();
        TradeCyclingManager.register();
        HoeTweaksManager.register();
        SilkierTouchManager.register();
        PetNamesManager.register();
        DespawningEggsManager.register();
        
        // Register commands
        AioCommands.register();
        
        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("AIO Mod fully loaded with all features!");
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerDataManager.saveAllPlayers();
            LOGGER.info("AIO Mod: All player data saved.");
        });
        
        // Player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerDataManager.loadPlayer(handler.getPlayer());
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerDataManager.savePlayer(handler.getPlayer());
            // Clean up sleep sooner tracking
            SleepSoonerManager.onPlayerDisconnect(handler.getPlayer().getUUID());
        });
        
        // Auto-save tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % (20 * 60 * 5) == 0) { // Every 5 minutes
                PlayerDataManager.saveAllPlayers();
            }
        });
        
        LOGGER.info("All-in-One Mod initialized successfully!");
    }
}
