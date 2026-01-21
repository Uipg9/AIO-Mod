package com.baesp.aio.client.hud;

import com.baesp.aio.AioMod;
import com.baesp.aio.client.gui.ClientDataCache;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HudRenderer {
    
    // Action bar floating messages
    private static final List<FloatingMessage> floatingMessages = new ArrayList<>();
    private static long lastMessageTime = 0;
    
    // Animation timing
    private static float pulseTimer = 0f;
    
    // HUD settings
    private static boolean sidebarEnabled = true;
    private static boolean actionBarEnabled = true;
    private static boolean clockEnabled = true;
    
    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
        AioMod.LOGGER.info("AIO HUD Renderer registered.");
    }
    
    public static void toggleSidebar() {
        sidebarEnabled = !sidebarEnabled;
    }
    
    public static void toggleClock() {
        clockEnabled = !clockEnabled;
    }
    
    public static boolean isSidebarEnabled() {
        return sidebarEnabled;
    }
    
    public static boolean isClockEnabled() {
        return clockEnabled;
    }
    
    /**
     * Add a floating message to display in action bar area
     */
    public static void addFloatingMessage(String text, int color) {
        // Prevent spam - minimum 100ms between messages
        long now = System.currentTimeMillis();
        if (now - lastMessageTime < 100) return;
        lastMessageTime = now;
        
        floatingMessages.add(new FloatingMessage(text, color, now));
        
        // Keep max 5 messages
        while (floatingMessages.size() > 5) {
            floatingMessages.remove(0);
        }
    }
    
    public static void addMoneyMessage(long amount) {
        if (amount > 0) {
            addFloatingMessage("+$" + formatNumber(amount), 0xFFFFD700);
        } else if (amount < 0) {
            addFloatingMessage("-$" + formatNumber(-amount), 0xFFFF5555);
        }
    }
    
    public static void addSoulXpMessage(int amount) {
        if (amount > 0) {
            addFloatingMessage("+" + amount + " Soul XP", 0xFF55FFFF);
        } else if (amount < 0) {
            addFloatingMessage(amount + " Soul XP", 0xFFFF5555);
        }
    }
    
    public static void addXpMessage(int amount) {
        if (amount > 0) {
            addFloatingMessage("+" + amount + " XP", 0xFF55FF55);
        }
    }
    
    public static void addCustomMessage(String text, int color) {
        addFloatingMessage(text, color);
    }
    
    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        if (client.screen != null) return; // Don't render when GUI is open
        
        float delta = tickCounter.getGameTimeDeltaTicks();
        pulseTimer += delta * 0.1f;
        
        Font font = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        // Render sidebar
        if (sidebarEnabled) {
            renderSidebar(graphics, font, screenWidth, screenHeight);
        }
        
        // Render clock
        if (clockEnabled) {
            renderClock(graphics, font, screenWidth, client);
        }
        
        // Render floating action bar messages
        if (actionBarEnabled) {
            renderFloatingMessages(graphics, font, screenWidth, screenHeight);
        }
    }
    
    private static void renderSidebar(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        ClientDataCache data = ClientDataCache.get();
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;
        
        // Sidebar position (top-right corner)
        int sidebarWidth = 120;
        int sidebarX = screenWidth - sidebarWidth - 5;
        int sidebarY = 5;
        int lineHeight = 12;
        int padding = 6;
        
        // Calculate sidebar height
        int lines = 9; // Title + 8 data lines
        int sidebarHeight = (lines * lineHeight) + (padding * 2);
        
        // Background with transparency
        graphics.fill(sidebarX, sidebarY, sidebarX + sidebarWidth, sidebarY + sidebarHeight, 0x88000000);
        
        // Border
        drawBorder(graphics, sidebarX, sidebarY, sidebarWidth, sidebarHeight, 0xFF555555);
        
        // Gold accent line at top
        graphics.fill(sidebarX + 1, sidebarY + 1, sidebarX + sidebarWidth - 1, sidebarY + 3, 0xFFFFD700);
        
        int textY = sidebarY + padding + 2;
        
        // Title with pulse effect
        float pulse = (float) (0.7f + 0.3f * Math.sin(pulseTimer));
        int titleColor = blendColor(0xFFFFD700, 0xFFFFFFFF, pulse);
        drawCenteredText(graphics, font, "§l⚔ AIO Status", sidebarX + sidebarWidth / 2, textY, titleColor);
        textY += lineHeight + 2;
        
        // Separator
        graphics.fill(sidebarX + 5, textY, sidebarX + sidebarWidth - 5, textY + 1, 0xFF444444);
        textY += 4;
        
        // Soul XP (with cyan icon)
        String soulXpText = "§b✦ §7Soul XP: §f" + formatNumber(data.soulXp);
        graphics.drawString(font, soulXpText, sidebarX + padding, textY, 0xFFFFFFFF, true);
        textY += lineHeight;
        
        // Shop Balance (with gold icon)
        String balanceText = "§6$ §7Balance: §e" + data.formatMoney();
        graphics.drawString(font, balanceText, sidebarX + padding, textY, 0xFFFFFFFF, true);
        textY += lineHeight;
        
        // Vanilla XP Level
        int xpLevel = player.experienceLevel;
        float xpProgress = player.experienceProgress;
        String xpText = "§a★ §7Level: §f" + xpLevel + " §8(" + (int)(xpProgress * 100) + "%)";
        graphics.drawString(font, xpText, sidebarX + padding, textY, 0xFFFFFFFF, true);
        textY += lineHeight;
        
        // Separator
        graphics.fill(sidebarX + 5, textY, sidebarX + sidebarWidth - 5, textY + 1, 0xFF444444);
        textY += 4;
        
        // Health
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int healthColor = health > maxHealth * 0.5 ? 0xFFFF5555 : (health > maxHealth * 0.25 ? 0xFFFFAA00 : 0xFF550000);
        String healthText = "§c❤ §7HP: §f" + (int)health + "/" + (int)maxHealth;
        graphics.drawString(font, healthText, sidebarX + padding, textY, 0xFFFFFFFF, true);
        textY += lineHeight;
        
        // Food/Hunger
        int food = player.getFoodData().getFoodLevel();
        String foodText = "§6🍖 §7Food: §f" + food + "/20";
        graphics.drawString(font, foodText, sidebarX + padding, textY, 0xFFFFFFFF, true);
        textY += lineHeight;
        
        // Coordinates (compact)
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        String coordText = "§8XYZ: " + x + ", " + y + ", " + z;
        graphics.drawString(font, coordText, sidebarX + padding, textY, 0xFF888888, true);
    }
    
    private static void renderClock(GuiGraphics graphics, Font font, int screenWidth, Minecraft client) {
        if (client.level == null) return;
        
        long dayTime = client.level.getDayTime() % 24000;
        int hours = (int) ((dayTime / 1000 + 6) % 24);
        int minutes = (int) ((dayTime % 1000) * 60 / 1000);
        
        String timeStr = String.format("%02d:%02d", hours, minutes);
        String period = (hours >= 6 && hours < 18) ? "☀" : "☽";
        int timeColor = (hours >= 6 && hours < 18) ? 0xFFFFDD55 : 0xFF8888FF;
        
        String clockText = period + " " + timeStr;
        int clockWidth = font.width(clockText) + 10;
        int clockX = (screenWidth - clockWidth) / 2;
        int clockY = 5;
        
        // Background
        graphics.fill(clockX - 2, clockY - 2, clockX + clockWidth + 2, clockY + 12, 0x88000000);
        drawBorder(graphics, clockX - 2, clockY - 2, clockWidth + 4, 14, 0xFF444444);
        
        graphics.drawString(font, clockText, clockX + 5, clockY, timeColor, true);
    }
    
    private static void renderFloatingMessages(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        int baseY = screenHeight - 60; // Above hotbar
        
        // Remove expired messages and render active ones
        Iterator<FloatingMessage> iter = floatingMessages.iterator();
        int index = 0;
        
        while (iter.hasNext()) {
            FloatingMessage msg = iter.next();
            long age = now - msg.startTime;
            
            if (age > 2000) { // 2 second lifespan
                iter.remove();
                continue;
            }
            
            // Calculate fade and position
            float progress = age / 2000f;
            float alpha = progress < 0.8f ? 1f : (1f - (progress - 0.8f) / 0.2f);
            float yOffset = progress * 30; // Float upward
            
            int y = (int) (baseY - yOffset - (index * 15));
            int x = screenWidth / 2;
            
            // Apply alpha to color
            int color = (msg.color & 0x00FFFFFF) | ((int)(alpha * 255) << 24);
            
            // Shadow for readability
            String text = msg.text;
            int textWidth = font.width(text);
            graphics.drawString(font, text, x - textWidth / 2 + 1, y + 1, 0x44000000, false);
            graphics.drawString(font, text, x - textWidth / 2, y, color, false);
            
            index++;
        }
    }
    
    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color); // Top
        graphics.fill(x, y + h - 1, x + w, y + h, color); // Bottom
        graphics.fill(x, y, x + 1, y + h, color); // Left
        graphics.fill(x + w - 1, y, x + w, y + h, color); // Right
    }
    
    private static void drawCenteredText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        int width = font.width(text);
        graphics.drawString(font, text, x - width / 2, y, color, true);
    }
    
    private static int blendColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 * ratio + r2 * (1 - ratio));
        int g = (int) (g1 * ratio + g2 * (1 - ratio));
        int b = (int) (b1 * ratio + b2 * (1 - ratio));
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    private static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    // Inner class for floating messages
    private static class FloatingMessage {
        final String text;
        final int color;
        final long startTime;
        
        FloatingMessage(String text, int color, long startTime) {
            this.text = text;
            this.color = color;
            this.startTime = startTime;
        }
    }
}
