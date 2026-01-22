package com.baesp.aio.client;

import com.baesp.aio.AioMod;
import com.baesp.aio.client.gui.AscendancyScreen;
import com.baesp.aio.client.gui.FeatureToggleScreen;
import com.baesp.aio.client.gui.ShopScreen;
import com.baesp.aio.client.gui.SkillsScreen;
import com.baesp.aio.client.hud.HudRenderer;
import com.baesp.aio.network.AioNetworkClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class AioKeybindings {
    // MAIN: Feature Toggle Menu
    public static KeyMapping OPEN_FEATURE_MENU;  // [ - Open Feature Toggle Menu
    
    // ROW 1: Utility Keybinds (G, H, J, K) - Near Squat Grow (C)
    public static KeyMapping TOGGLE_SQUAT_GROW;  // G - Squat Grow Toggle
    public static KeyMapping TOGGLE_HUD;         // H - Toggle HUD Sidebar
    public static KeyMapping OPEN_SHOP;          // J - Shop
    public static KeyMapping OPEN_SKILLS;        // K - Skills Menu
    
    // ROW 2: Additional Utilities (B, N, M)
    public static KeyMapping TOGGLE_BRIGHTNESS;  // B - Full Brightness Toggle
    public static KeyMapping TOGGLE_VOID_MAGNET; // N - Void Magnet Toggle
    public static KeyMapping OPEN_ASCENDANCY;    // M - Ascendancy Tree
    
    // Misc
    public static KeyMapping OPEN_QUESTS;        // Q - Daily Quests (future)
    
    // Client-side state
    private static boolean fullBrightnessEnabled = false;
    private static boolean voidMagnetEnabled = false;
    
    private static final KeyMapping.Category AIO_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "keybinds")
    );
    
    public static void register() {
        // MAIN: Feature Toggle Menu - '[' key
        OPEN_FEATURE_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.feature_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,  // '[' key
            AIO_CATEGORY
        ));
        
        // ROW 1: Utility Keybinds (G, H, J, K)
        TOGGLE_SQUAT_GROW = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.squatgrow",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            AIO_CATEGORY
        ));
        
        TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            AIO_CATEGORY
        ));
        
        OPEN_SHOP = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.shop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            AIO_CATEGORY
        ));
        
        OPEN_SKILLS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.skills",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            AIO_CATEGORY
        ));
        
        // ROW 2: Additional Utilities
        TOGGLE_BRIGHTNESS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.brightness",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            AIO_CATEGORY
        ));
        
        TOGGLE_VOID_MAGNET = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.void_magnet",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            AIO_CATEGORY
        ));
        
        OPEN_ASCENDANCY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.ascendancy",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            AIO_CATEGORY
        ));
        
        // Misc
        OPEN_QUESTS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.quests",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA, // < key, future use
            AIO_CATEGORY
        ));
        
        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(AioKeybindings::handleKeyInputs);
        
        AioMod.LOGGER.info("AIO Keybindings registered.");
    }
    
    private static void handleKeyInputs(Minecraft client) {
        if (client.player == null) return;
        
        // '[' - Open Feature Toggle Menu (close if already open)
        while (OPEN_FEATURE_MENU.consumeClick()) {
            if (client.screen instanceof FeatureToggleScreen) {
                client.setScreen(null);  // Close if already open
            } else {
                AioNetworkClient.sendRequestData();
                client.setScreen(new FeatureToggleScreen());
            }
        }
        
        // J - Toggle Shop (close if already open)
        while (OPEN_SHOP.consumeClick()) {
            if (client.screen instanceof ShopScreen) {
                client.setScreen(null);  // Close if already open
            } else {
                AioNetworkClient.sendRequestData();
                client.setScreen(new ShopScreen());
            }
        }
        
        // H - Toggle HUD Sidebar
        while (TOGGLE_HUD.consumeClick()) {
            HudRenderer.toggleSidebar();
            String status = HudRenderer.isSidebarEnabled() ? "§aEnabled" : "§cDisabled";
            client.player.displayClientMessage(
                Component.literal("§6[AIO] §7HUD Sidebar: " + status), true
            );
        }
        
        // K - Toggle Skills (close if already open)
        while (OPEN_SKILLS.consumeClick()) {
            if (client.screen instanceof SkillsScreen) {
                client.setScreen(null);  // Close if already open
            } else {
                AioNetworkClient.sendRequestData();
                client.setScreen(new SkillsScreen());
            }
        }
        
        // G - Toggle Squat Grow (removed - use [ menu instead)
        // Keybind still exists for players who prefer it, but not recommended
        while (TOGGLE_SQUAT_GROW.consumeClick()) {
            // Deprecated - use [ menu. Silent ignore.
        }
        
        // B - Toggle Full Brightness (removed - use [ menu instead)
        while (TOGGLE_BRIGHTNESS.consumeClick()) {
            // Deprecated - use [ menu. Silent ignore.
        }
        
        // N - Toggle Void Magnet (removed - use [ menu instead)
        while (TOGGLE_VOID_MAGNET.consumeClick()) {
            // Deprecated - use [ menu. Silent ignore.
        }
        
        // M - Toggle Ascendancy (close if already open)
        while (OPEN_ASCENDANCY.consumeClick()) {
            if (client.screen instanceof AscendancyScreen) {
                client.setScreen(null);  // Close if already open
            } else {
                AioNetworkClient.sendRequestData();
                client.setScreen(new AscendancyScreen());
            }
        }
        
        // Quests (placeholder for now)
        while (OPEN_QUESTS.consumeClick()) {
            client.player.displayClientMessage(
                Component.literal("§6[AIO] §7Daily Quests coming soon!"), true
            );
        }
    }
    
    public static boolean isFullBrightnessEnabled() {
        return fullBrightnessEnabled;
    }
    
    public static boolean isVoidMagnetEnabled() {
        return voidMagnetEnabled;
    }
    
    public static void setVoidMagnetEnabled(boolean enabled) {
        voidMagnetEnabled = enabled;
    }
}
