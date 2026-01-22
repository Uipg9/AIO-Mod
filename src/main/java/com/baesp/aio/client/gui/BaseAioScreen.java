package com.baesp.aio.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Base screen class using pure DrawContext API
 * No external GUI libraries - full control over rendering
 */
public abstract class BaseAioScreen extends Screen {
    
    // Window dimensions
    protected int windowWidth;
    protected int windowHeight;
    protected int windowX;
    protected int windowY;
    
    // Help system
    protected boolean showingHelp = false;
    protected boolean helpButtonHovered = false;
    
    // Color palette - AIO Theme
    protected static final int COLOR_BACKGROUND = 0xE8101018;
    protected static final int COLOR_BACKGROUND_LIGHT = 0xE8181820;
    protected static final int COLOR_BORDER_GOLD = 0xFFFFD700;
    protected static final int COLOR_BORDER_PURPLE = 0xFF9932CC;
    protected static final int COLOR_BORDER_GREEN = 0xFF32CD32;
    protected static final int COLOR_BORDER_BLUE = 0xFF4169E1;
    protected static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    protected static final int COLOR_TEXT_GRAY = 0xFF888888;
    protected static final int COLOR_TEXT_GOLD = 0xFFFFD700;
    protected static final int COLOR_TEXT_GREEN = 0xFF55FF55;
    protected static final int COLOR_TEXT_RED = 0xFFFF5555;
    protected static final int COLOR_TEXT_AQUA = 0xFF55FFFF;
    protected static final int COLOR_TEXT_PURPLE = 0xFFAA00FF;
    protected static final int COLOR_BUTTON_BG = 0xCC222228;
    protected static final int COLOR_BUTTON_HOVER = 0xCC333340;
    protected static final int COLOR_BUTTON_DISABLED = 0xCC111115;
    
    protected BaseAioScreen(Component title) {
        super(title);
    }
    
    @Override
    protected void init() {
        super.init();
        calculateWindowSize();
        clearWidgets();
        addButtons();
        
        // Add help button as a widget
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            onHelpButtonClick();
        }).bounds(windowX + 8, windowY + 8, 16, 16).build());
    }
    
    /**
     * Override to set custom window dimensions
     */
    protected void calculateWindowSize() {
        windowWidth = Math.min(400, width - 40);
        windowHeight = Math.min(300, height - 40);
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }
    
    /**
     * Override to add button widgets
     */
    protected abstract void addButtons();
    
    /**
     * Override to render custom content
     */
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta);
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 1. Dim background
        graphics.fill(0, 0, width, height, 0xB0000000);
        
        // 2. Draw window with fancy border
        drawFancyWindow(graphics);
        
        // 3. Draw title
        drawTitle(graphics);
        
        // 4. Render buttons (vanilla widgets)
        super.render(graphics, mouseX, mouseY, delta);
        
        // 5. Custom content on top (unless showing help)
        if (!showingHelp) {
            renderContent(graphics, mouseX, mouseY, delta);
        }
        
        // 6. Draw help button
        renderHelpButton(graphics, mouseX, mouseY);
        
        // 7. Draw help overlay if showing
        if (showingHelp) {
            renderHelpOverlay(graphics, mouseX, mouseY);
        }
    }
    
    /**
     * Renders the help button (?) in top-left corner
     */
    protected void renderHelpButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int helpX = windowX + 8;
        int helpY = windowY + 8;
        int helpSize = 16;
        
        helpButtonHovered = isMouseOver(mouseX, mouseY, helpX, helpY, helpSize, helpSize);
        
        // Button background
        int bgColor = showingHelp ? 0xCC336633 : (helpButtonHovered ? 0xCC444466 : 0xCC222244);
        graphics.fill(helpX, helpY, helpX + helpSize, helpY + helpSize, bgColor);
        
        // Border
        int borderColor = showingHelp ? COLOR_TEXT_GREEN : (helpButtonHovered ? COLOR_TEXT_AQUA : 0xFF666688);
        drawBorder(graphics, helpX, helpY, helpSize, helpSize, borderColor);
        
        // Question mark
        int textColor = showingHelp ? COLOR_TEXT_GREEN : (helpButtonHovered ? COLOR_TEXT_AQUA : COLOR_TEXT_WHITE);
        drawCenteredText(graphics, "?", helpX + helpSize / 2, helpY + 4, textColor);
        
        // Tooltip when hovered (and not showing help)
        if (helpButtonHovered && !showingHelp) {
            List<String> tip = new ArrayList<>();
            tip.add("§eClick for Help Guide");
            tip.add("§7Learn how to use this menu");
            drawTooltip(graphics, mouseX, mouseY, tip);
        }
    }
    
    /**
     * Renders the help overlay with instructions
     */
    protected void renderHelpOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> helpLines = getHelpText();
        if (helpLines.isEmpty()) return;
        
        int padding = 20;
        int overlayX = windowX + padding;
        int overlayY = windowY + 35;
        int overlayW = windowWidth - (padding * 2);
        int overlayH = windowHeight - 55;
        
        // Dark overlay background
        graphics.fill(overlayX, overlayY, overlayX + overlayW, overlayY + overlayH, 0xF0101020);
        
        // Border
        drawBorder(graphics, overlayX, overlayY, overlayW, overlayH, COLOR_TEXT_AQUA);
        
        // Title
        String helpTitle = "§b§l📖 HELP GUIDE";
        drawCenteredText(graphics, helpTitle, windowX + windowWidth / 2, overlayY + 8, COLOR_TEXT_WHITE);
        
        // Divider
        graphics.fill(overlayX + 10, overlayY + 22, overlayX + overlayW - 10, overlayY + 23, 0x80FFFFFF);
        
        // Help text
        int lineY = overlayY + 30;
        for (String line : helpLines) {
            if (lineY + 11 > overlayY + overlayH - 25) break; // Don't overflow
            graphics.drawString(font, line, overlayX + 10, lineY, COLOR_TEXT_WHITE, true);
            lineY += 12;
        }
        
        // Close instruction
        String closeText = "§7Click §e?§7 or press §eESC§7 to close help";
        drawCenteredText(graphics, closeText, windowX + windowWidth / 2, overlayY + overlayH - 15, COLOR_TEXT_GRAY);
    }
    
    /**
     * Override this to provide help text for your screen
     */
    protected List<String> getHelpText() {
        List<String> help = new ArrayList<>();
        help.add("§7No help available for this screen.");
        return help;
    }
    
    /**
     * Handle help button click
     */
    protected void onHelpButtonClick() {
        showingHelp = !showingHelp;
        playClickSound();
    }
    
    protected void drawFancyWindow(GuiGraphics graphics) {
        int x1 = windowX;
        int y1 = windowY;
        int x2 = windowX + windowWidth;
        int y2 = windowY + windowHeight;
        
        // Main background with subtle gradient effect
        graphics.fill(x1, y1, x2, y2, COLOR_BACKGROUND);
        
        // Inner lighter area
        graphics.fill(x1 + 3, y1 + 3, x2 - 3, y2 - 3, COLOR_BACKGROUND_LIGHT);
        
        // Double border effect
        int borderColor = getBorderColor();
        drawBorder(graphics, x1, y1, windowWidth, windowHeight, borderColor);
        drawBorder(graphics, x1 + 2, y1 + 2, windowWidth - 4, windowHeight - 4, 
            (borderColor & 0x00FFFFFF) | 0x80000000); // Same color, 50% alpha
        
        // Corner accents (golden/themed triangles)
        drawCornerAccents(graphics, borderColor);
    }
    
    protected int getBorderColor() {
        return COLOR_BORDER_GOLD; // Override for different screens
    }
    
    protected void drawCornerAccents(GuiGraphics graphics, int color) {
        int size = 8;
        int x1 = windowX;
        int y1 = windowY;
        int x2 = windowX + windowWidth;
        int y2 = windowY + windowHeight;
        
        // Top-left corner accent
        for (int i = 0; i < size; i++) {
            graphics.fill(x1 + i, y1 + i, x1 + size - i, y1 + i + 1, color);
        }
        
        // Top-right corner accent
        for (int i = 0; i < size; i++) {
            graphics.fill(x2 - size + i, y1 + i, x2 - i, y1 + i + 1, color);
        }
        
        // Bottom-left corner accent
        for (int i = 0; i < size; i++) {
            graphics.fill(x1 + i, y2 - i - 1, x1 + size - i, y2 - i, color);
        }
        
        // Bottom-right corner accent
        for (int i = 0; i < size; i++) {
            graphics.fill(x2 - size + i, y2 - i - 1, x2 - i, y2 - i, color);
        }
    }
    
    protected void drawTitle(GuiGraphics graphics) {
        String titleText = getTitle().getString();
        int textWidth = font.width(titleText);
        int titleX = windowX + (windowWidth - textWidth) / 2;
        int titleY = windowY + 12;
        
        // Title background bar
        graphics.fill(windowX + 20, titleY - 4, windowX + windowWidth - 20, titleY + 14, 0x80000000);
        
        // Decorative lines
        int lineColor = getBorderColor();
        graphics.fill(windowX + 20, titleY - 4, windowX + windowWidth - 20, titleY - 3, lineColor);
        graphics.fill(windowX + 20, titleY + 13, windowX + windowWidth - 20, titleY + 14, lineColor);
        
        // Title text with shadow
        graphics.drawString(font, titleText, titleX, titleY, COLOR_TEXT_WHITE, true);
    }
    
    // === UTILITY METHODS ===
    
    protected void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color); // Top
        graphics.fill(x, y + h - 1, x + w, y + h, color); // Bottom
        graphics.fill(x, y, x + 1, y + h, color); // Left
        graphics.fill(x + w - 1, y, x + w, y + h, color); // Right
    }
    
    protected void drawCenteredText(GuiGraphics graphics, String text, int centerX, int y, int color) {
        int textWidth = font.width(text);
        graphics.drawString(font, text, centerX - textWidth / 2, y, color, true);
    }
    
    protected void drawRightAlignedText(GuiGraphics graphics, String text, int rightX, int y, int color) {
        int textWidth = font.width(text);
        graphics.drawString(font, text, rightX - textWidth, y, color, true);
    }
    
    protected boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
    
    protected void drawButton(GuiGraphics graphics, int x, int y, int w, int h, 
                              String text, boolean hovered, boolean enabled) {
        int bgColor = !enabled ? COLOR_BUTTON_DISABLED : (hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG);
        int borderColor = !enabled ? 0xFF444444 : (hovered ? COLOR_BORDER_GOLD : 0xFF666666);
        int textColor = !enabled ? 0xFF666666 : (hovered ? COLOR_TEXT_GOLD : COLOR_TEXT_WHITE);
        
        // Background
        graphics.fill(x, y, x + w, y + h, bgColor);
        
        // Border
        drawBorder(graphics, x, y, w, h, borderColor);
        
        // Text centered
        drawCenteredText(graphics, text, x + w / 2, y + (h - 8) / 2, textColor);
    }
    
    protected void drawProgressBar(GuiGraphics graphics, int x, int y, int w, int h,
                                   double progress, int fillColor, int bgColor) {
        // Background
        graphics.fill(x, y, x + w, y + h, bgColor);
        
        // Fill
        int fillWidth = (int) (w * Math.min(1.0, Math.max(0.0, progress)));
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + h, fillColor);
        }
        
        // Border
        drawBorder(graphics, x, y, w, h, 0xFF444444);
    }
    
    protected void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, java.util.List<String> lines) {
        if (lines.isEmpty()) return;
        
        int tooltipWidth = 0;
        for (String line : lines) {
            tooltipWidth = Math.max(tooltipWidth, font.width(line));
        }
        tooltipWidth += 10;
        int tooltipHeight = lines.size() * 11 + 8;
        
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        
        // Keep on screen
        if (tooltipX + tooltipWidth > width) tooltipX = mouseX - tooltipWidth - 4;
        if (tooltipY + tooltipHeight > height) tooltipY = height - tooltipHeight;
        if (tooltipY < 0) tooltipY = 0;
        
        // Background
        graphics.fill(tooltipX - 3, tooltipY - 3, tooltipX + tooltipWidth + 3, tooltipY + tooltipHeight + 3, 0xF0100010);
        
        // Border
        drawBorder(graphics, tooltipX - 3, tooltipY - 3, tooltipWidth + 6, tooltipHeight + 6, 0xFF5000AA);
        
        // Text
        int lineY = tooltipY;
        for (String line : lines) {
            graphics.drawString(font, line, tooltipX, lineY, COLOR_TEXT_WHITE, true);
            lineY += 11;
        }
    }
    
    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
    
    protected void playSuccessSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F)
        );
    }
    
    protected void playErrorSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F)
        );
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
