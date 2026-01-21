package com.baesp.aio.client.gui;

import com.baesp.aio.network.AioNetworkClient;
import com.baesp.aio.rpg.economy.ShopData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop Screen - Expanded shop with 10 categories and many items
 * Pure DrawContext rendering with tabs and scrolling
 */
public class ShopScreen extends BaseAioScreen {
    
    private int selectedCategory = 0;
    private int scrollOffset = 0;
    private int hoveredItem = -1;
    private int categoryScrollOffset = 0;
    private static final int VISIBLE_ITEMS = 6;
    private static final int VISIBLE_CATEGORIES = 5;
    
    public ShopScreen() {
        super(Component.literal("§6⭐ Shop ⭐"));
    }
    
    @Override
    protected void calculateWindowSize() {
        windowWidth = Math.min(480, width - 40);
        windowHeight = Math.min(420, height - 40);
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }
    
    @Override
    protected int getBorderColor() {
        return COLOR_BORDER_GOLD;
    }
    
    @Override
    protected void addButtons() {
        // Close button
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            playClickSound();
            onClose();
        }).bounds(windowX + windowWidth - 30, windowY + 8, 20, 16).build());
        
        // Category scroll left
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            if (categoryScrollOffset > 0) {
                categoryScrollOffset--;
                playClickSound();
            }
        }).bounds(windowX + 8, windowY + 65, 16, 24).build());
        
        // Category scroll right
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            int maxCatScroll = Math.max(0, ShopData.CATEGORIES.length - VISIBLE_CATEGORIES);
            if (categoryScrollOffset < maxCatScroll) {
                categoryScrollOffset++;
                playClickSound();
            }
        }).bounds(windowX + windowWidth - 24, windowY + 65, 16, 24).build());
        
        // Category tabs
        for (int i = 0; i < VISIBLE_CATEGORIES; i++) {
            final int idx = i;
            int tabWidth = (windowWidth - 60) / VISIBLE_CATEGORIES;
            int tabX = windowX + 28 + (i * tabWidth);
            addRenderableWidget(Button.builder(Component.literal(""), btn -> {
                int catIdx = categoryScrollOffset + idx;
                if (catIdx < ShopData.CATEGORIES.length && selectedCategory != catIdx) {
                    selectedCategory = catIdx;
                    scrollOffset = 0;
                    playClickSound();
                }
            }).bounds(tabX, windowY + 65, tabWidth - 2, 24).build());
        }
        
        // Item scroll buttons
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            if (scrollOffset > 0) {
                scrollOffset--;
                playClickSound();
            }
        }).bounds(windowX + windowWidth - 35, windowY + 130, 20, 16).build());
        
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            if (selectedCategory < ShopData.ITEMS.length) {
                int maxScroll = Math.max(0, ShopData.ITEMS[selectedCategory].length - VISIBLE_ITEMS);
                if (scrollOffset < maxScroll) {
                    scrollOffset++;
                    playClickSound();
                }
            }
        }).bounds(windowX + windowWidth - 35, windowY + windowHeight - 60, 20, 16).build());
        
        // Item buy buttons
        for (int i = 0; i < VISIBLE_ITEMS; i++) {
            final int idx = i;
            int btnY = windowY + 135 + (i * 38);
            addRenderableWidget(Button.builder(Component.literal(""), btn -> {
                int itemIndex = scrollOffset + idx;
                if (selectedCategory < ShopData.ITEMS.length && itemIndex < ShopData.ITEMS[selectedCategory].length) {
                    AioNetworkClient.sendBuyItem(selectedCategory, itemIndex);
                    playClickSound();
                }
            }).bounds(windowX + 15, btnY, windowWidth - 55, 34).build());
        }
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ClientDataCache data = ClientDataCache.get();
        hoveredItem = -1;
        
        // Balance display
        renderBalance(graphics, data);
        
        // Category tabs
        renderCategoryTabs(graphics, mouseX, mouseY);
        
        // Items list
        renderItems(graphics, mouseX, mouseY, data);
        
        // Scroll buttons
        renderScrollButtons(graphics, mouseX, mouseY);
        
        // Close button
        renderCloseButton(graphics, mouseX, mouseY);
        
        // Tooltip
        if (hoveredItem >= 0) {
            renderItemTooltip(graphics, mouseX, mouseY, hoveredItem, data);
        }
    }
    
    private void renderBalance(GuiGraphics graphics, ClientDataCache data) {
        int balanceY = windowY + 42;
        
        // Balance background
        int balanceX = windowX + 20;
        int balanceW = windowWidth - 40;
        graphics.fill(balanceX, balanceY, balanceX + balanceW, balanceY + 18, 0xCC1A2A1A);
        drawBorder(graphics, balanceX, balanceY, balanceW, 18, COLOR_BORDER_GREEN);
        
        // Balance text
        String balanceText = "§a💰 Balance: §f" + data.formatMoney();
        drawCenteredText(graphics, balanceText, windowX + windowWidth / 2, balanceY + 5, COLOR_TEXT_GREEN);
    }
    
    private void renderCategoryTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabY = windowY + 65;
        int tabWidth = (windowWidth - 60) / VISIBLE_CATEGORIES;
        
        // Left arrow
        int leftX = windowX + 8;
        boolean canScrollLeft = categoryScrollOffset > 0;
        boolean leftHovered = isMouseOver(mouseX, mouseY, leftX, tabY, 16, 24);
        graphics.fill(leftX, tabY, leftX + 16, tabY + 24, canScrollLeft ? (leftHovered ? 0xCC444444 : 0xCC333333) : 0xCC222222);
        drawBorder(graphics, leftX, tabY, 16, 24, canScrollLeft ? (leftHovered ? COLOR_BORDER_GOLD : 0xFF666666) : 0xFF333333);
        drawCenteredText(graphics, "◀", leftX + 8, tabY + 8, canScrollLeft ? COLOR_TEXT_WHITE : 0xFF555555);
        
        // Tabs
        for (int i = 0; i < VISIBLE_CATEGORIES; i++) {
            int catIdx = categoryScrollOffset + i;
            if (catIdx >= ShopData.CATEGORIES.length) break;
            
            int tabX = windowX + 28 + (i * tabWidth);
            int tabW = tabWidth - 2;
            int tabH = 24;
            
            boolean selected = catIdx == selectedCategory;
            boolean hovered = isMouseOver(mouseX, mouseY, tabX, tabY, tabW, tabH);
            
            // Tab background
            int bgColor;
            if (selected) {
                bgColor = (ShopData.CATEGORY_COLORS[catIdx] & 0x00FFFFFF) | 0x40000000;
            } else if (hovered) {
                bgColor = 0xCC333333;
            } else {
                bgColor = 0xCC222222;
            }
            graphics.fill(tabX, tabY, tabX + tabW, tabY + tabH, bgColor);
            
            // Tab border
            if (selected) {
                drawBorder(graphics, tabX, tabY, tabW, tabH, ShopData.CATEGORY_COLORS[catIdx]);
                graphics.fill(tabX, tabY + tabH - 2, tabX + tabW, tabY + tabH, ShopData.CATEGORY_COLORS[catIdx]);
            } else {
                drawBorder(graphics, tabX, tabY, tabW, tabH, hovered ? 0xFF666666 : 0xFF444444);
            }
            
            // Icon
            String tabText = ShopData.CATEGORY_ICONS[catIdx];
            int textColor = selected ? ShopData.CATEGORY_COLORS[catIdx] : (hovered ? COLOR_TEXT_WHITE : COLOR_TEXT_GRAY);
            drawCenteredText(graphics, tabText, tabX + tabW / 2, tabY + 4, textColor);
            
            // Abbreviated category name
            String shortName = ShopData.CATEGORIES[catIdx].length() > 6 ? 
                ShopData.CATEGORIES[catIdx].substring(0, 5) + ".." : ShopData.CATEGORIES[catIdx];
            graphics.drawString(font, shortName, tabX + (tabW - font.width(shortName)) / 2, tabY + 14, 
                selected ? COLOR_TEXT_WHITE : COLOR_TEXT_GRAY, true);
        }
        
        // Right arrow
        int rightX = windowX + windowWidth - 24;
        int maxCatScroll = Math.max(0, ShopData.CATEGORIES.length - VISIBLE_CATEGORIES);
        boolean canScrollRight = categoryScrollOffset < maxCatScroll;
        boolean rightHovered = isMouseOver(mouseX, mouseY, rightX, tabY, 16, 24);
        graphics.fill(rightX, tabY, rightX + 16, tabY + 24, canScrollRight ? (rightHovered ? 0xCC444444 : 0xCC333333) : 0xCC222222);
        drawBorder(graphics, rightX, tabY, 16, 24, canScrollRight ? (rightHovered ? COLOR_BORDER_GOLD : 0xFF666666) : 0xFF333333);
        drawCenteredText(graphics, "▶", rightX + 8, tabY + 8, canScrollRight ? COLOR_TEXT_WHITE : 0xFF555555);
        
        // Category name below tabs
        String catName = "§f" + ShopData.CATEGORIES[selectedCategory];
        drawCenteredText(graphics, catName, windowX + windowWidth / 2, tabY + 28, COLOR_TEXT_WHITE);
    }
    
    private void renderItems(GuiGraphics graphics, int mouseX, int mouseY, ClientDataCache data) {
        if (selectedCategory >= ShopData.ITEMS.length) return;
        
        ShopData.ShopItem[] items = ShopData.ITEMS[selectedCategory];
        int startY = windowY + 135;
        int itemH = 38;
        
        for (int i = 0; i < VISIBLE_ITEMS; i++) {
            int itemIndex = scrollOffset + i;
            if (itemIndex >= items.length) break;
            
            int y = startY + (i * itemH);
            int x = windowX + 15;
            int w = windowWidth - 55;
            int h = itemH - 4;
            
            boolean hovered = isMouseOver(mouseX, mouseY, x, y, w, h);
            if (hovered) hoveredItem = itemIndex;
            
            renderItemRow(graphics, itemIndex, x, y, w, h, hovered, data, items);
        }
    }
    
    private void renderItemRow(GuiGraphics graphics, int index, int x, int y, int w, int h,
                                boolean hovered, ClientDataCache data, ShopData.ShopItem[] items) {
        ShopData.ShopItem item = items[index];
        boolean canAfford = data.money >= item.price;
        
        // Background
        int bgColor = hovered ? 0xCC2A2A35 : 0xCC1E1E26;
        graphics.fill(x, y, x + w, y + h, bgColor);
        
        // Border
        int borderColor = hovered ? (canAfford ? COLOR_BORDER_GREEN : COLOR_BORDER_GOLD) : 0xFF555555;
        drawBorder(graphics, x, y, w, h, borderColor);
        
        // Icon background
        int iconSize = h - 8;
        graphics.fill(x + 4, y + 4, x + 4 + iconSize, y + 4 + iconSize, 0xFF2A2A35);
        drawBorder(graphics, x + 4, y + 4, iconSize, iconSize, ShopData.CATEGORY_COLORS[selectedCategory]);
        
        // Icon
        drawCenteredText(graphics, item.icon, x + 4 + iconSize / 2, y + h / 2 - 4, COLOR_TEXT_WHITE);
        
        // Item name with count
        int textX = x + iconSize + 14;
        String nameText = item.count > 1 ? item.name + " x" + item.count : item.name;
        graphics.drawString(font, nameText, textX, y + 8, COLOR_TEXT_WHITE, true);
        
        // Stock/availability indicator
        String stockText = "§7In Stock";
        graphics.drawString(font, stockText, textX, y + 20, 0xFF888888, true);
        
        // Price
        String priceText = "§e$" + formatNumber(item.price);
        int priceColor = canAfford ? COLOR_TEXT_GREEN : COLOR_TEXT_RED;
        int priceX = x + w - font.width(priceText) - 8;
        graphics.drawString(font, priceText, priceX, y + h / 2 - 4, priceColor, true);
        
        // Buy indicator on hover
        if (hovered) {
            String buyText = canAfford ? "§a[BUY]" : "§c[Can't afford]";
            graphics.drawString(font, buyText, priceX - font.width(buyText) - 10, y + h / 2 - 4, canAfford ? COLOR_TEXT_GREEN : COLOR_TEXT_RED, true);
        }
    }
    
    private void renderScrollButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selectedCategory >= ShopData.ITEMS.length) return;
        ShopData.ShopItem[] items = ShopData.ITEMS[selectedCategory];
        int maxScroll = Math.max(0, items.length - VISIBLE_ITEMS);
        
        // Up button
        int upX = windowX + windowWidth - 35;
        int upY = windowY + 135;
        boolean upHovered = isMouseOver(mouseX, mouseY, upX, upY, 20, 16);
        boolean canUp = scrollOffset > 0;
        
        graphics.fill(upX, upY, upX + 20, upY + 16, canUp ? (upHovered ? 0xCC444444 : 0xCC333333) : 0xCC222222);
        drawBorder(graphics, upX, upY, 20, 16, canUp ? (upHovered ? COLOR_BORDER_GOLD : 0xFF666666) : 0xFF333333);
        drawCenteredText(graphics, "▲", upX + 10, upY + 4, canUp ? COLOR_TEXT_WHITE : 0xFF555555);
        
        // Down button
        int downX = windowX + windowWidth - 35;
        int downY = windowY + windowHeight - 75;
        boolean downHovered = isMouseOver(mouseX, mouseY, downX, downY, 20, 16);
        boolean canDown = scrollOffset < maxScroll;
        
        graphics.fill(downX, downY, downX + 20, downY + 16, canDown ? (downHovered ? 0xCC444444 : 0xCC333333) : 0xCC222222);
        drawBorder(graphics, downX, downY, 20, 16, canDown ? (downHovered ? COLOR_BORDER_GOLD : 0xFF666666) : 0xFF333333);
        drawCenteredText(graphics, "▼", downX + 10, downY + 4, canDown ? COLOR_TEXT_WHITE : 0xFF555555);
        
        // Position indicator
        int first = scrollOffset + 1;
        int last = Math.min(scrollOffset + VISIBLE_ITEMS, items.length);
        String posText = "(" + first + "-" + last + "/" + items.length + ")";
        drawCenteredText(graphics, posText, downX + 10, downY - 12, COLOR_TEXT_GRAY);
    }
    
    private void renderCloseButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int closeX = windowX + windowWidth - 30;
        int closeY = windowY + 8;
        boolean closeHovered = isMouseOver(mouseX, mouseY, closeX, closeY, 20, 16);
        
        graphics.fill(closeX, closeY, closeX + 20, closeY + 16, closeHovered ? 0xCC553333 : 0xCC332222);
        drawBorder(graphics, closeX, closeY, 20, 16, closeHovered ? 0xFFFF5555 : 0xFF882222);
        drawCenteredText(graphics, "×", closeX + 10, closeY + 4, closeHovered ? 0xFFFF5555 : 0xFFAA5555);
    }
    
    private void renderItemTooltip(GuiGraphics graphics, int mouseX, int mouseY, int index, ClientDataCache data) {
        if (selectedCategory >= ShopData.ITEMS.length) return;
        ShopData.ShopItem[] items = ShopData.ITEMS[selectedCategory];
        if (index >= items.length) return;
        
        ShopData.ShopItem item = items[index];
        boolean canAfford = data.money >= item.price;
        
        List<String> lines = new ArrayList<>();
        lines.add("§e" + item.name + (item.count > 1 ? " x" + item.count : ""));
        lines.add("");
        lines.add("§7Category: §f" + ShopData.CATEGORIES[selectedCategory]);
        lines.add("§7Item ID: §8" + item.itemId);
        lines.add("");
        lines.add("§7Price: §e$" + formatNumber(item.price));
        lines.add("§7Your Balance: §f$" + data.formatMoney());
        lines.add("");
        
        if (canAfford) {
            lines.add("§a✔ Click to buy!");
            lines.add("§7Remaining: §f$" + formatNumber(data.money - item.price));
        } else {
            lines.add("§c✘ Not enough money!");
            lines.add("§7Need: §c$" + formatNumber(item.price - data.money) + " more");
        }
        
        drawTooltip(graphics, mouseX, mouseY, lines);
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (selectedCategory >= ShopData.ITEMS.length) return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
        int maxScroll = Math.max(0, ShopData.ITEMS[selectedCategory].length - VISIBLE_ITEMS);
        
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
