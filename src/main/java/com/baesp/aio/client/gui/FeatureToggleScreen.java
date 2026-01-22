package com.baesp.aio.client.gui;

import com.baesp.aio.client.AioKeybindings;
import com.baesp.aio.client.hud.HudRenderer;
import com.baesp.aio.network.AioNetworkClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Feature Toggle GUI - Centralized control panel for all AIO features
 * Opened with '[' key
 */
public class FeatureToggleScreen extends Screen {
    
    // Feature states (synced with AioKeybindings)
    private boolean squatGrowEnabled = false;
    private boolean fullBrightnessEnabled = false;
    private boolean voidMagnetEnabled = false;
    private boolean hudSidebarEnabled = true;
    private boolean autoToolSwapEnabled = true;
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;
    
    public FeatureToggleScreen() {
        super(Component.literal("AIO Feature Toggles"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Get current states
        fullBrightnessEnabled = AioKeybindings.isFullBrightnessEnabled();
        voidMagnetEnabled = AioKeybindings.isVoidMagnetEnabled();
        hudSidebarEnabled = HudRenderer.isSidebarEnabled();
        squatGrowEnabled = ClientDataCache.get().squatGrowEnabled;
        autoToolSwapEnabled = ClientFeatureState.autoToolSwapEnabled;
        
        int centerX = this.width / 2;
        int startY = 60;
        
        // Title is drawn in render
        
        // Feature Toggle Buttons
        int y = startY;
        
        // Squat Grow Toggle
        addRenderableWidget(Button.builder(
            getToggleText("🌱 Squat Grow", squatGrowEnabled),
            btn -> {
                AioNetworkClient.sendToggleSquatGrow();
                squatGrowEnabled = !squatGrowEnabled;
                btn.setMessage(getToggleText("🌱 Squat Grow", squatGrowEnabled));
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        
        // Full Brightness Toggle
        addRenderableWidget(Button.builder(
            getToggleText("☀ Full Brightness", fullBrightnessEnabled),
            btn -> {
                fullBrightnessEnabled = !fullBrightnessEnabled;
                toggleFullBrightness(fullBrightnessEnabled);
                btn.setMessage(getToggleText("☀ Full Brightness", fullBrightnessEnabled));
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        
        // Void Magnet Toggle
        addRenderableWidget(Button.builder(
            getToggleText("⚡ Void Magnet", voidMagnetEnabled),
            btn -> {
                voidMagnetEnabled = !voidMagnetEnabled;
                AioNetworkClient.sendToggleVoidMagnet(voidMagnetEnabled);
                AioKeybindings.setVoidMagnetEnabled(voidMagnetEnabled);
                btn.setMessage(getToggleText("⚡ Void Magnet", voidMagnetEnabled));
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        
        // HUD Sidebar Toggle
        addRenderableWidget(Button.builder(
            getToggleText("📊 HUD Sidebar", hudSidebarEnabled),
            btn -> {
                hudSidebarEnabled = !hudSidebarEnabled;
                HudRenderer.toggleSidebar();
                btn.setMessage(getToggleText("📊 HUD Sidebar", hudSidebarEnabled));
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        
        // Auto Tool Swap Toggle
        addRenderableWidget(Button.builder(
            getToggleText("⚒ Auto Tool Swap", autoToolSwapEnabled),
            btn -> {
                autoToolSwapEnabled = !autoToolSwapEnabled;
                ClientFeatureState.autoToolSwapEnabled = autoToolSwapEnabled;
                btn.setMessage(getToggleText("⚒ Auto Tool Swap", autoToolSwapEnabled));
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING * 2;
        
        // ---- Quick Access Buttons ----
        
        // Open Shop
        addRenderableWidget(Button.builder(
            Component.literal("§6🛒 Open Shop"),
            btn -> {
                this.minecraft.setScreen(new ShopScreen());
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH/2 - 5, BUTTON_HEIGHT).build());
        
        // Open Skills
        addRenderableWidget(Button.builder(
            Component.literal("§b⚔ Open Skills"),
            btn -> {
                this.minecraft.setScreen(new SkillsScreen());
            }
        ).bounds(centerX + 5, y, BUTTON_WIDTH/2 - 5, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        
        // Open Ascendancy
        addRenderableWidget(Button.builder(
            Component.literal("§5✦ Open Ascendancy"),
            btn -> {
                AioNetworkClient.sendRequestData();
                this.minecraft.setScreen(new AscendancyScreen());
            }
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH/2 - 5, BUTTON_HEIGHT).build());
        
        // Open Warp Hub
        addRenderableWidget(Button.builder(
            Component.literal("§d🌀 Warp Hub"),
            btn -> {
                // Send command to server to teleport to warp hub
                if (this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand("warphub");
                    this.onClose();
                }
            }
        ).bounds(centerX + 5, y, BUTTON_WIDTH/2 - 5, BUTTON_HEIGHT).build());
        y += BUTTON_SPACING * 2;
        
        // Close Button
        addRenderableWidget(Button.builder(
            Component.literal("§7Close §8[Press '[' or ESC]"),
            btn -> this.onClose()
        ).bounds(centerX - BUTTON_WIDTH/2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }
    
    private Component getToggleText(String name, boolean enabled) {
        String status = enabled ? "§a✓ ON" : "§c✗ OFF";
        return Component.literal(name + "  [" + status + "§r]");
    }
    
    private void toggleFullBrightness(boolean enabled) {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (enabled) {
                this.minecraft.player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 
                    Integer.MAX_VALUE, 
                    0, 
                    false, 
                    false, 
                    false
                ));
            } else {
                this.minecraft.player.removeEffect(MobEffects.NIGHT_VISION);
            }
            // Update the keybindings state
            try {
                var field = AioKeybindings.class.getDeclaredField("fullBrightnessEnabled");
                field.setAccessible(true);
                field.setBoolean(null, enabled);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Call super.render first - this handles background blur in 1.21.11
        super.render(graphics, mouseX, mouseY, delta);
        
        // Draw title
        graphics.drawCenteredString(
            this.font, 
            "§6§l✦ AIO Feature Toggles ✦", 
            this.width / 2, 
            20, 
            0xFFFFFF
        );
        
        // Draw subtitle
        graphics.drawCenteredString(
            this.font, 
            "§7Configure your AIO Mod features", 
            this.width / 2, 
            35, 
            0xAAAAAA
        );
        
        // Draw help text at bottom
        graphics.drawCenteredString(
            this.font, 
            "§8Press '[' to open this menu anytime", 
            this.width / 2, 
            this.height - 30, 
            0x888888
        );
        
        // Note: super.render() called at beginning of method, not here
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
