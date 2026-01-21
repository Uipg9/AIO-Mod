package com.baesp.aio.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Ascendancy Screen - Soul progression and prestige upgrades
 * Beautiful pure DrawContext rendering
 */
public class AscendancyScreen extends BaseAioScreen {
    
    private static final String[] UPGRADE_NAMES = {
        "Vitality", "Swiftness", "Might", "Resilience", 
        "Haste", "Fortune", "Wisdom", "Reach", "Keeper"
    };
    private static final String[] UPGRADE_ICONS = {
        "§c❤", "§b⚡", "§6⚔", "§9🛡", 
        "§e⛏", "§a✦", "§d✧", "§f➤", "§6📦"
    };
    private static final String[] UPGRADE_DESCRIPTIONS = {
        "+2 Max Health per level",
        "+5% Movement Speed per level",
        "+5% Attack Damage per level",
        "+3% Damage Reduction per level",
        "+5% Mining Speed per level",
        "+5% Bonus Drops per level",
        "+10% XP Gain per level",
        "+0.5 Block Reach per level",
        "Keep items on death"
    };
    private static final int[] UPGRADE_COLORS = {
        0xFFFF5555, 0xFF55FFFF, 0xFFFFAA00, 0xFF5555FF,
        0xFFFFFF55, 0xFF55FF55, 0xFFFF55FF, 0xFFFFFFFF, 0xFFFFAA00
    };
    
    private static final int MAX_LEVEL = 10;
    private int hoveredUpgrade = -1;
    private boolean ascendHovered = false;
    private int scrollOffset = 0;
    private static final int VISIBLE_UPGRADES = 5;
    
    public AscendancyScreen() {
        super(Component.literal("§5✦ Ascendancy ✦"));
    }
    
    @Override
    protected void calculateWindowSize() {
        windowWidth = Math.min(420, width - 40);
        windowHeight = Math.min(380, height - 40);
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }
    
    @Override
    protected int getBorderColor() {
        return COLOR_BORDER_PURPLE;
    }
    
    @Override
    protected void addButtons() {
        // Close button
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            playClickSound();
            onClose();
        }).bounds(windowX + windowWidth - 30, windowY + 8, 20, 16).build());
        
        // Scroll up button
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                playClickSound();
            }
        }).bounds(windowX + windowWidth - 35, windowY + 125, 20, 16).build());
        
        // Scroll down button
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            int maxScroll = Math.max(0, UPGRADE_NAMES.length - VISIBLE_UPGRADES);
            if (scrollOffset < maxScroll) {
                scrollOffset++;
                playClickSound();
            }
        }).bounds(windowX + windowWidth - 35, windowY + 290, 20, 16).build());
        
        // Ascend button (handled in render)
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            // Send ascension request to server
            playClickSound();
        }).bounds(windowX + windowWidth / 2 - 60, windowY + windowHeight - 45, 120, 28).build());
        
        // Upgrade buttons
        for (int i = 0; i < VISIBLE_UPGRADES; i++) {
            final int idx = i;
            int btnY = windowY + 130 + (i * 32);
            addRenderableWidget(Button.builder(Component.literal(""), btn -> {
                int upgradeIndex = scrollOffset + idx;
                if (upgradeIndex < UPGRADE_NAMES.length) {
                    // Send upgrade request to server
                    playClickSound();
                }
            }).bounds(windowX + 15, btnY, windowWidth - 60, 28).build());
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ClientDataCache data = ClientDataCache.get();
        hoveredUpgrade = -1;
        
        // Player info section
        renderPlayerInfo(graphics, data, mouseX, mouseY);
        
        // Upgrades section
        renderUpgrades(graphics, data, mouseX, mouseY);
        
        // Scroll buttons
        renderScrollButtons(graphics, mouseX, mouseY);
        
        // Ascend button
        renderAscendButton(graphics, mouseX, mouseY, data);
        
        // Close button
        renderCloseButton(graphics, mouseX, mouseY);
        
        // Tooltip
        if (hoveredUpgrade >= 0) {
            renderUpgradeTooltip(graphics, mouseX, mouseY, hoveredUpgrade, data);
        }
    }
    
    private void renderPlayerInfo(GuiGraphics graphics, ClientDataCache data, int mouseX, int mouseY) {
        int infoY = windowY + 40;
        int centerX = windowX + windowWidth / 2;
        
        // Soul level display - large centered
        String soulText = "§5Soul Level §d" + data.soulLevel;
        drawCenteredText(graphics, soulText, centerX, infoY, COLOR_TEXT_PURPLE);
        
        // XP bar
        int barX = windowX + 40;
        int barY = infoY + 18;
        int barW = windowWidth - 80;
        int barH = 14;
        
        double progress = data.soulXpToNextLevel > 0 ? (double) data.soulXp / data.soulXpToNextLevel : 0;
        
        // Bar background
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);
        
        // Bar fill with purple gradient
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, 0xFF9932CC);
            graphics.fill(barX, barY, barX + fillW, barY + 3, 0x60FFFFFF);
        }
        
        // Bar border
        drawBorder(graphics, barX, barY, barW, barH, COLOR_BORDER_PURPLE);
        
        // XP text
        String xpText = data.soulXp + " / " + data.soulXpToNextLevel + " Soul XP";
        drawCenteredText(graphics, xpText, centerX, barY + 3, COLOR_TEXT_WHITE);
        
        // Stats row
        int statsY = barY + 22;
        
        // Ascensions
        String ascText = "§5⚜ " + data.ascensionCount + " Ascensions";
        graphics.drawString(font, ascText, windowX + 20, statsY, COLOR_TEXT_PURPLE, true);
        
        // Prestige points
        String prestigeText = "§e★ " + data.prestigePoints + " Prestige Points";
        int prestigeX = windowX + windowWidth - font.width(prestigeText) - 20;
        graphics.drawString(font, prestigeText, prestigeX, statsY, COLOR_TEXT_GOLD, true);
        
        // Divider line
        int divY = statsY + 14;
        graphics.fill(windowX + 20, divY, windowX + windowWidth - 20, divY + 1, 0x40FFFFFF);
        
        // Section title
        String upgradesTitle = "§f⬇ Prestige Upgrades ⬇";
        drawCenteredText(graphics, upgradesTitle, centerX, divY + 6, COLOR_TEXT_GRAY);
    }
    
    private void renderUpgrades(GuiGraphics graphics, ClientDataCache data, int mouseX, int mouseY) {
        int startY = windowY + 130;
        int upgradeH = 32;
        
        for (int i = 0; i < VISIBLE_UPGRADES; i++) {
            int upgradeIndex = scrollOffset + i;
            if (upgradeIndex >= UPGRADE_NAMES.length) break;
            
            int y = startY + (i * upgradeH);
            int x = windowX + 15;
            int w = windowWidth - 60;
            int h = upgradeH - 4;
            
            boolean hovered = isMouseOver(mouseX, mouseY, x, y, w, h);
            if (hovered) hoveredUpgrade = upgradeIndex;
            
            renderUpgradeRow(graphics, upgradeIndex, x, y, w, h, hovered, data);
        }
    }
    
    private void renderUpgradeRow(GuiGraphics graphics, int index, int x, int y, int w, int h,
                                   boolean hovered, ClientDataCache data) {
        int level = data.getUpgradeLevel(index);
        int cost = getUpgradeCost(level + 1);
        boolean canAfford = data.prestigePoints >= cost && level < MAX_LEVEL;
        boolean maxed = level >= MAX_LEVEL;
        int color = UPGRADE_COLORS[index];
        
        // Background
        int bgColor = maxed ? 0x40FFD700 : (hovered ? 0xCC2A2A35 : 0xCC1E1E26);
        graphics.fill(x, y, x + w, y + h, bgColor);
        
        // Border
        int borderColor = maxed ? COLOR_BORDER_GOLD : (hovered ? color : 0xFF555555);
        drawBorder(graphics, x, y, w, h, borderColor);
        
        // Icon
        int iconSize = h - 4;
        graphics.fill(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, (color & 0x00FFFFFF) | 0x30000000);
        drawCenteredText(graphics, UPGRADE_ICONS[index], x + 2 + iconSize / 2, y + h / 2 - 4, COLOR_TEXT_WHITE);
        
        // Name
        int textX = x + iconSize + 10;
        graphics.drawString(font, UPGRADE_NAMES[index], textX, y + 4, maxed ? COLOR_TEXT_GOLD : COLOR_TEXT_WHITE, true);
        
        // Level pips
        int pipX = textX;
        int pipY = y + 16;
        for (int i = 0; i < MAX_LEVEL; i++) {
            int pipColor = i < level ? color : 0xFF333333;
            graphics.fill(pipX + i * 10, pipY, pipX + i * 10 + 8, pipY + 4, pipColor);
        }
        
        // Cost or MAX
        String rightText;
        int rightColor;
        if (maxed) {
            rightText = "§6★ MAX";
            rightColor = COLOR_TEXT_GOLD;
        } else {
            rightText = cost + " PP";
            rightColor = canAfford ? COLOR_TEXT_GREEN : COLOR_TEXT_RED;
        }
        int rightX = x + w - font.width(rightText) - 8;
        graphics.drawString(font, rightText, rightX, y + h / 2 - 4, rightColor, true);
    }
    
    private void renderScrollButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int maxScroll = Math.max(0, UPGRADE_NAMES.length - VISIBLE_UPGRADES);
        
        // Up button
        int upX = windowX + windowWidth - 35;
        int upY = windowY + 125;
        boolean upHovered = isMouseOver(mouseX, mouseY, upX, upY, 20, 16);
        boolean canUp = scrollOffset > 0;
        
        graphics.fill(upX, upY, upX + 20, upY + 16, canUp ? (upHovered ? 0xCC444444 : 0xCC333333) : 0xCC222222);
        drawBorder(graphics, upX, upY, 20, 16, canUp ? (upHovered ? COLOR_BORDER_PURPLE : 0xFF666666) : 0xFF333333);
        drawCenteredText(graphics, "▲", upX + 10, upY + 4, canUp ? COLOR_TEXT_WHITE : 0xFF555555);
        
        // Down button
        int downX = windowX + windowWidth - 35;
        int downY = windowY + 290;
        boolean downHovered = isMouseOver(mouseX, mouseY, downX, downY, 20, 16);
        boolean canDown = scrollOffset < maxScroll;
        
        graphics.fill(downX, downY, downX + 20, downY + 16, canDown ? (downHovered ? 0xCC444444 : 0xCC333333) : 0xCC222222);
        drawBorder(graphics, downX, downY, 20, 16, canDown ? (downHovered ? COLOR_BORDER_PURPLE : 0xFF666666) : 0xFF333333);
        drawCenteredText(graphics, "▼", downX + 10, downY + 4, canDown ? COLOR_TEXT_WHITE : 0xFF555555);
        
        // Position indicator
        int first = scrollOffset + 1;
        int last = Math.min(scrollOffset + VISIBLE_UPGRADES, UPGRADE_NAMES.length);
        String posText = "(" + first + "-" + last + "/" + UPGRADE_NAMES.length + ")";
        drawCenteredText(graphics, posText, downX + 10, downY - 12, COLOR_TEXT_GRAY);
    }
    
    private void renderAscendButton(GuiGraphics graphics, int mouseX, int mouseY, ClientDataCache data) {
        int btnX = windowX + windowWidth / 2 - 60;
        int btnY = windowY + windowHeight - 45;
        int btnW = 120;
        int btnH = 28;
        
        ascendHovered = isMouseOver(mouseX, mouseY, btnX, btnY, btnW, btnH);
        
        // Gradient background
        int bgColor = ascendHovered ? 0xCC4A2A5A : 0xCC3A1A4A;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bgColor);
        
        // Glow effect when hovered
        if (ascendHovered) {
            graphics.fill(btnX + 2, btnY + 2, btnX + btnW - 2, btnY + 4, 0x40FFFFFF);
        }
        
        // Border
        drawBorder(graphics, btnX, btnY, btnW, btnH, ascendHovered ? 0xFFAA55FF : COLOR_BORDER_PURPLE);
        
        // Text
        String btnText = "§5⚜ ASCEND ⚜";
        drawCenteredText(graphics, btnText, btnX + btnW / 2, btnY + btnH / 2 - 4, ascendHovered ? 0xFFDD88FF : 0xFFAA55FF);
        
        // Warning text below
        if (ascendHovered) {
            String warnText = "§c⚠ Resets progress for Prestige Points";
            drawCenteredText(graphics, warnText, windowX + windowWidth / 2, btnY + btnH + 5, COLOR_TEXT_RED);
        }
    }
    
    private void renderCloseButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int closeX = windowX + windowWidth - 30;
        int closeY = windowY + 8;
        boolean closeHovered = isMouseOver(mouseX, mouseY, closeX, closeY, 20, 16);
        
        graphics.fill(closeX, closeY, closeX + 20, closeY + 16, closeHovered ? 0xCC553333 : 0xCC332222);
        drawBorder(graphics, closeX, closeY, 20, 16, closeHovered ? 0xFFFF5555 : 0xFF882222);
        drawCenteredText(graphics, "×", closeX + 10, closeY + 4, closeHovered ? 0xFFFF5555 : 0xFFAA5555);
    }
    
    private void renderUpgradeTooltip(GuiGraphics graphics, int mouseX, int mouseY, int index, ClientDataCache data) {
        List<String> lines = new ArrayList<>();
        int level = data.getUpgradeLevel(index);
        int cost = getUpgradeCost(level + 1);
        boolean maxed = level >= MAX_LEVEL;
        
        lines.add("§e" + UPGRADE_NAMES[index]);
        lines.add("");
        lines.add("§7" + UPGRADE_DESCRIPTIONS[index]);
        lines.add("");
        lines.add("§7Level: §f" + level + "§7/§f" + MAX_LEVEL);
        
        if (!maxed) {
            lines.add("");
            boolean canAfford = data.prestigePoints >= cost;
            lines.add("§7Cost: " + (canAfford ? "§a" : "§c") + cost + " Prestige Points");
            lines.add(canAfford ? "§aClick to upgrade!" : "§cNot enough points!");
        } else {
            lines.add("");
            lines.add("§6★ Maximum level reached!");
        }
        
        drawTooltip(graphics, mouseX, mouseY, lines);
    }
    
    private int getUpgradeCost(int level) {
        return level * 2; // Simple formula: 2, 4, 6, 8, 10...
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int maxScroll = Math.max(0, UPGRADE_NAMES.length - VISIBLE_UPGRADES);
        
        if (vAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (vAmount < 0 && scrollOffset < maxScroll) {
            scrollOffset++;
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }
}
