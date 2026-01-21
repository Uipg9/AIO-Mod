package com.baesp.aio.client;

import com.baesp.aio.AioMod;
import com.baesp.aio.client.gui.AscendancyScreen;
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
    // ROW 1: GUI Utilities (G, H, J, K)
    public static KeyMapping OPEN_SHOP;          // G - Shop
    public static KeyMapping TOGGLE_HUD;         // H - Toggle HUD Sidebar
    public static KeyMapping OPEN_ASCENDANCY;    // J - Ascendancy Tree
    public static KeyMapping OPEN_SKILLS;        // K - Skills Menu
    
    // ROW 2: Feature Toggles (B, N, M)
    public static KeyMapping TOGGLE_BRIGHTNESS;  // B - Full Brightness Toggle
    public static KeyMapping TOGGLE_VOID_MAGNET; // N - Void Magnet Toggle
    public static KeyMapping TOGGLE_SQUAT_GROW;  // M - Squat Grow Toggle
    
    // Misc
    public static KeyMapping OPEN_QUESTS;        // Q - Daily Quests (future)
    
    // Client-side state
    private static boolean fullBrightnessEnabled = false;
    private static boolean voidMagnetEnabled = false;
    
    private static final KeyMapping.Category AIO_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "keybinds")
    );
    
    public static void register() {
        // ROW 1: GUI Utilities
        OPEN_SHOP = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.shop",
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
        
        OPEN_ASCENDANCY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.ascendancy",
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
        
        // ROW 2: Feature Toggles
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
        
        TOGGLE_SQUAT_GROW = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aio.squatgrow",
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
        
        // G - Open Shop
        while (OPEN_SHOP.consumeClick()) {
            AioNetworkClient.sendRequestData();
            client.setScreen(new ShopScreen());
        }
        
        // H - Toggle HUD Sidebar
        while (TOGGLE_HUD.consumeClick()) {
            HudRenderer.toggleSidebar();
            String status = HudRenderer.isSidebarEnabled() ? "§aEnabled" : "§cDisabled";
            client.player.displayClientMessage(
                Component.literal("§6[AIO] §7HUD Sidebar: " + status), true
            );
        }
        
        // J - Open Ascendancy
        while (OPEN_ASCENDANCY.consumeClick()) {
            AioNetworkClient.sendRequestData();
            client.setScreen(new AscendancyScreen());
        }
        
        // K - Open Skills
        while (OPEN_SKILLS.consumeClick()) {
            AioNetworkClient.sendRequestData();
            client.setScreen(new SkillsScreen());
        }
        
        // B - Toggle Full Brightness (Night Vision Effect)
        while (TOGGLE_BRIGHTNESS.consumeClick()) {
            fullBrightnessEnabled = !fullBrightnessEnabled;
            if (fullBrightnessEnabled) {
                // Apply infinite night vision (client-side visual only)
                client.player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 
                    Integer.MAX_VALUE, 
                    0, 
                    false, 
                    false, 
                    false
                ));
                client.player.displayClientMessage(
                    Component.literal("§6[AIO] §aFull Brightness: §fEnabled"), true
                );
            } else {
                client.player.removeEffect(MobEffects.NIGHT_VISION);
                client.player.displayClientMessage(
                    Component.literal("§6[AIO] §cFull Brightness: §fDisabled"), true
                );
            }
            HudRenderer.addCustomMessage(
                fullBrightnessEnabled ? "☀ Brightness ON" : "☀ Brightness OFF",
                fullBrightnessEnabled ? 0xFFFFFF55 : 0xFF888888
            );
        }
        
        // N - Toggle Void Magnet
        while (TOGGLE_VOID_MAGNET.consumeClick()) {
            voidMagnetEnabled = !voidMagnetEnabled;
            // Notify server about void magnet state
            AioNetworkClient.sendToggleVoidMagnet(voidMagnetEnabled);
            String status = voidMagnetEnabled ? "§aEnabled" : "§cDisabled";
            client.player.displayClientMessage(
                Component.literal("§6[AIO] §5Void Magnet: " + status), true
            );
            HudRenderer.addCustomMessage(
                voidMagnetEnabled ? "⚡ Void Magnet ON" : "⚡ Void Magnet OFF",
                voidMagnetEnabled ? 0xFF9955FF : 0xFF888888
            );
        }
        
        // M - Toggle Squat Grow
        while (TOGGLE_SQUAT_GROW.consumeClick()) {
            AioNetworkClient.sendToggleSquatGrow();
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
