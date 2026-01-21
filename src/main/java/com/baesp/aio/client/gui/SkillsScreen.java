package com.baesp.aio.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Skills Screen - Pure DrawContext rendering
 * Shows all player skills with XP bars and bonuses
 */
public class SkillsScreen extends BaseAioScreen {
    
    private static final String[] SKILL_NAMES = {"Farming", "Combat", "Defense", "Smithing", "Woodcutting", "Mining"};
    private static final String[] SKILL_ICONS = {"§a⚘", "§c⚔", "§9🛡", "§6⚒", "§2🪓", "§7⛏"};
    private static final String[] SKILL_DESCRIPTIONS = {
        "Bonus crop drops from harvesting",
        "Increased damage to enemies",
        "Reduced incoming damage",
        "Better furnace results",
        "Extra logs from trees",
        "Chance for bonus ores"
    };
    private static final int[] SKILL_COLORS = {
        0xFF55FF55, // Farming - Green
        0xFFFF5555, // Combat - Red
        0xFF5555FF, // Defense - Blue
        0xFFFFAA00, // Smithing - Orange
        0xFF00AA00, // Woodcutting - Dark Green
        0xFFAAAAAA  // Mining - Gray
    };
    
    private int hoveredSkill = -1;
    
    public SkillsScreen() {
        super(Component.literal("§6✦ Skills ✦"));
    }
    
    @Override
    protected void calculateWindowSize() {
        windowWidth = Math.min(380, width - 40);
        windowHeight = Math.min(320, height - 40);
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }
    
    @Override
    protected int getBorderColor() {
        return COLOR_BORDER_GREEN;
    }
    
    @Override
    protected void addButtons() {
        // Close button
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            playClickSound();
            onClose();
        }).bounds(windowX + windowWidth - 30, windowY + 8, 20, 16).build());
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ClientDataCache data = ClientDataCache.get();
        hoveredSkill = -1;
        
        int startY = windowY + 45;
        int skillHeight = 40;
        int padding = 15;
        
        // Render each skill
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int skillY = startY + (i * skillHeight);
            int skillX = windowX + padding;
            int skillW = windowWidth - (padding * 2);
            
            boolean hovered = isMouseOver(mouseX, mouseY, skillX, skillY, skillW, skillHeight - 5);
            if (hovered) hoveredSkill = i;
            
            renderSkillRow(graphics, i, skillX, skillY, skillW, skillHeight - 5, hovered, data);
        }
        
        // Close button visual
        int closeX = windowX + windowWidth - 30;
        int closeY = windowY + 8;
        boolean closeHovered = isMouseOver(mouseX, mouseY, closeX, closeY, 20, 16);
        graphics.fill(closeX, closeY, closeX + 20, closeY + 16, closeHovered ? 0xCC553333 : 0xCC332222);
        drawBorder(graphics, closeX, closeY, 20, 16, closeHovered ? 0xFFFF5555 : 0xFF882222);
        drawCenteredText(graphics, "×", closeX + 10, closeY + 4, closeHovered ? 0xFFFF5555 : 0xFFAA5555);
        
        // Tooltip for hovered skill
        if (hoveredSkill >= 0) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§e" + SKILL_NAMES[hoveredSkill]);
            tooltip.add("");
            tooltip.add("§7" + SKILL_DESCRIPTIONS[hoveredSkill]);
            tooltip.add("");
            int level = data.getSkillLevel(hoveredSkill);
            double bonus = level * 5.0;
            tooltip.add("§7Current Bonus: §a+" + String.format("%.0f", bonus) + "%");
            tooltip.add("§8Left-click to view details");
            drawTooltip(graphics, mouseX, mouseY, tooltip);
        }
    }
    
    private void renderSkillRow(GuiGraphics graphics, int index, int x, int y, int w, int h, 
                                 boolean hovered, ClientDataCache data) {
        int level = data.getSkillLevel(index);
        int xp = data.getSkillXp(index);
        int xpNeeded = data.getXpForLevel(level);
        double progress = xpNeeded > 0 ? (double) xp / xpNeeded : 0;
        int color = SKILL_COLORS[index];
        
        // Background
        int bgColor = hovered ? 0xCC282830 : 0xCC1C1C22;
        graphics.fill(x, y, x + w, y + h, bgColor);
        
        // Border
        int borderColor = hovered ? color : (color & 0x00FFFFFF) | 0x80000000;
        drawBorder(graphics, x, y, w, h, borderColor);
        
        // Skill icon background
        int iconSize = h - 6;
        int iconX = x + 3;
        int iconY = y + 3;
        graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, (color & 0x00FFFFFF) | 0x40000000);
        drawBorder(graphics, iconX, iconY, iconSize, iconSize, color);
        
        // Skill icon (emoji-like text)
        drawCenteredText(graphics, SKILL_ICONS[index], iconX + iconSize / 2, iconY + iconSize / 2 - 4, COLOR_TEXT_WHITE);
        
        // Skill name
        int textX = iconX + iconSize + 10;
        graphics.drawString(font, "§f" + SKILL_NAMES[index], textX, y + 5, COLOR_TEXT_WHITE, true);
        
        // Level indicator
        String levelText = "Lv." + level;
        int levelX = x + w - font.width(levelText) - 10;
        graphics.drawString(font, levelText, levelX, y + 5, color, true);
        
        // XP bar
        int barX = textX;
        int barY = y + 18;
        int barW = w - iconSize - 30;
        int barH = 10;
        
        // Bar background
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);
        
        // Bar fill with gradient effect
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, color);
            // Shine effect
            graphics.fill(barX, barY, barX + fillW, barY + 2, (color & 0x00FFFFFF) | 0x60FFFFFF);
        }
        
        // Bar border
        drawBorder(graphics, barX, barY, barW, barH, 0xFF444444);
        
        // XP text
        String xpText = xp + "/" + xpNeeded + " XP";
        int xpTextX = barX + (barW - font.width(xpText)) / 2;
        graphics.drawString(font, xpText, xpTextX, barY + 1, COLOR_TEXT_WHITE, true);
        
        // Bonus percentage
        double bonus = level * 5.0;
        String bonusText = "§a+" + String.format("%.0f", bonus) + "%";
        graphics.drawString(font, bonusText, levelX - font.width(bonusText) - 5, y + 5, COLOR_TEXT_GREEN, true);
    }
    
    // Note: Mouse click handling is done via Button widgets
}
