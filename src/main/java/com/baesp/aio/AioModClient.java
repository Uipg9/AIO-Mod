package com.baesp.aio;

import com.baesp.aio.client.AioKeybindings;
import com.baesp.aio.client.hud.HudRenderer;
import com.baesp.aio.network.AioNetworkClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AioModClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        AioMod.LOGGER.info("Initializing AIO Mod client...");
        
        // Register keybindings
        AioKeybindings.register();
        
        // Register HUD renderer
        HudRenderer.register();
        
        // Register client networking
        AioNetworkClient.register();
        
        AioMod.LOGGER.info("AIO Mod client initialized!");
    }
}
