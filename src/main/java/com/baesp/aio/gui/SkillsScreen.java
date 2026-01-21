package com.baesp.aio.gui;

import com.baesp.aio.data.PlayerDataManager;
import com.baesp.aio.rpg.SkillsData;
import com.baesp.aio.rpg.SkillsManager;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class SkillsScreen extends SimpleGui {
    private final ServerPlayer player;
    
    public SkillsScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x3, player, false);
        this.player = player;
        
        setTitle(Component.literal("§6⚔ Skills ⚔"));
        setupGui();
    }
    
    private void setupGui() {
        SkillsData skills = PlayerDataManager.getData(player).skills;
        
        // Fill background
        for (int i = 0; i < 27; i++) {
            setSlot(i, new GuiElementBuilder()
                .setItem(Items.BLACK_STAINED_GLASS_PANE)
                .setName(Component.literal(""))
                .build()
            );
        }
        
        // Green accent corners
        for (int slot : new int[]{0, 8, 18, 26}) {
            setSlot(slot, new GuiElementBuilder()
                .setItem(Items.LIME_STAINED_GLASS_PANE)
                .setName(Component.literal(""))
                .build()
            );
        }
        
        // Skills
        String[] skillList = {
            SkillsData.SKILL_FARMING,
            SkillsData.SKILL_COMBAT,
            SkillsData.SKILL_DEFENSE,
            SkillsData.SKILL_SMITHING,
            SkillsData.SKILL_WOODCUTTING,
            SkillsData.SKILL_MINING
        };
        
        int[] slots = {10, 11, 12, 14, 15, 16};
        
        for (int i = 0; i < skillList.length; i++) {
            String skill = skillList[i];
            int level = skills.getSkillLevel(skill);
            int xp = skills.getSkillXp(skill);
            int xpNeeded = skills.getXpForLevel(level);
            double bonus = skills.getSkillBonus(skill) * 100;
            
            setSlot(slots[i], new GuiElementBuilder()
                .setItem(getSkillItem(skill))
                .setName(Component.literal("§a" + SkillsManager.formatSkillName(skill)))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("§7Level: §e" + level + "§7/§e10"))
                .addLoreLine(Component.literal("§7XP: §b" + xp + "§7/§b" + xpNeeded))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("§7Bonus: §a+" + String.format("%.0f", bonus) + "%"))
                .addLoreLine(Component.literal("§8" + getSkillDescription(skill)))
                .addLoreLine(Component.literal(""))
                .addLoreLine(Component.literal("§eClick for details"))
                .setCallback((index, type, action) -> {
                    // Show detailed skill info
                    player.sendSystemMessage(Component.literal(""));
                    player.sendSystemMessage(Component.literal("§6═══ " + SkillsManager.formatSkillName(skill) + " Details §6═══"));
                    player.sendSystemMessage(Component.literal("§7Level: §e" + level + "§7/§e10"));
                    player.sendSystemMessage(Component.literal("§7XP: §b" + xp + "§7/§b" + xpNeeded));
                    player.sendSystemMessage(Component.literal("§7Bonus: §a+" + String.format("%.0f", bonus) + "%"));
                    player.sendSystemMessage(Component.literal("§7" + getSkillDescription(skill)));
                    player.sendSystemMessage(Component.literal("§7XP Sources: §f" + getSkillSources(skill)));
                    player.sendSystemMessage(Component.literal(""));
                })
                .build()
            );
        }
        
        // Guide button
        setSlot(18, new GuiElementBuilder()
            .setItem(Items.BOOK)
            .setName(Component.literal("§e§l? Guide"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§6═══ Skills System ═══"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§e★ Leveling Skills:"))
            .addLoreLine(Component.literal("§7• Perform related actions"))
            .addLoreLine(Component.literal("§7• Gain skill XP"))
            .addLoreLine(Component.literal("§7• Level up for bonuses"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§a★ Skill Bonuses:"))
            .addLoreLine(Component.literal("§7• +5% bonus per level"))
            .addLoreLine(Component.literal("§7• Max level: 10"))
            .addLoreLine(Component.literal("§7• Permanent progress"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§b★ Click Skills:"))
            .addLoreLine(Component.literal("§7• See detailed information"))
            .addLoreLine(Component.literal("§7• View XP sources"))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("§bClick to close this guide"))
            .setCallback((index, type, action) -> setupGui())
            .build()
        );
        
        // Close button
        setSlot(22, new GuiElementBuilder()
            .setItem(Items.BARRIER)
            .setName(Component.literal("§cClose"))
            .setCallback((index, type, action) -> close())
            .build()
        );
    }
    
    private net.minecraft.world.item.Item getSkillItem(String skill) {
        return switch (skill) {
            case SkillsData.SKILL_FARMING -> Items.WHEAT;
            case SkillsData.SKILL_COMBAT -> Items.DIAMOND_SWORD;
            case SkillsData.SKILL_DEFENSE -> Items.SHIELD;
            case SkillsData.SKILL_SMITHING -> Items.ANVIL;
            case SkillsData.SKILL_WOODCUTTING -> Items.IRON_AXE;
            case SkillsData.SKILL_MINING -> Items.IRON_PICKAXE;
            default -> Items.PAPER;
        };
    }
    
    private String getSkillDescription(String skill) {
        return switch (skill) {
            case SkillsData.SKILL_FARMING -> "Bonus crop drops";
            case SkillsData.SKILL_COMBAT -> "Bonus attack damage";
            case SkillsData.SKILL_DEFENSE -> "Bonus damage reduction";
            case SkillsData.SKILL_SMITHING -> "Better crafting results";
            case SkillsData.SKILL_WOODCUTTING -> "Faster wood chopping";
            case SkillsData.SKILL_MINING -> "Faster mining speed";
            default -> "";
        };
    }
    
    private String getSkillSources(String skill) {
        return switch (skill) {
            case SkillsData.SKILL_FARMING -> "Harvesting crops, planting seeds";
            case SkillsData.SKILL_COMBAT -> "Fighting monsters, killing mobs";
            case SkillsData.SKILL_DEFENSE -> "Taking damage from enemies";
            case SkillsData.SKILL_SMITHING -> "Crafting items, using anvils";
            case SkillsData.SKILL_WOODCUTTING -> "Breaking logs, chopping trees";
            case SkillsData.SKILL_MINING -> "Mining ores, breaking stone";
            default -> "";
        };
    }
    
    public static void open(ServerPlayer player) {
        new SkillsScreen(player).open();
    }
}
