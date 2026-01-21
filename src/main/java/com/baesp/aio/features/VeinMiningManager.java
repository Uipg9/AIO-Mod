package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.rpg.SkillsManager;
import com.baesp.aio.rpg.economy.EconomyManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Vein Mining & Lumberjack System
 * 
 * When holding SHIFT and breaking an ore or log:
 * - Vein Mining: Breaks all connected ore blocks of the same type
 * - Lumberjack: Breaks all connected logs of the same type (tree felling)
 * 
 * Features:
 * - Integrates with XP systems (Mining skill for ores, Woodcutting for logs)
 * - Awards money based on blocks broken
 * - Respects tool durability
 * - Maximum vein size to prevent lag
 * 
 * Inspired by Vein Miner, TreeCapitator, and similar mods.
 */
public class VeinMiningManager {
    
    // Maximum blocks that can be broken in one vein
    private static final int MAX_VEIN_SIZE = 64;
    private static final int MAX_TREE_SIZE = 128;
    
    // Block categories and their XP/money values
    private static final Set<Block> ORE_BLOCKS = new HashSet<>();
    private static final Set<Block> LOG_BLOCKS = new HashSet<>();
    
    // Money per block broken
    private static final Map<Block, Integer> BLOCK_VALUES = new HashMap<>();
    
    // Currently processing (to prevent recursion)
    private static boolean isProcessing = false;
    
    static {
        // Ore blocks for vein mining
        ORE_BLOCKS.add(Blocks.COAL_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_COAL_ORE);
        ORE_BLOCKS.add(Blocks.IRON_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_IRON_ORE);
        ORE_BLOCKS.add(Blocks.COPPER_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_COPPER_ORE);
        ORE_BLOCKS.add(Blocks.GOLD_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_GOLD_ORE);
        ORE_BLOCKS.add(Blocks.NETHER_GOLD_ORE);
        ORE_BLOCKS.add(Blocks.REDSTONE_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        ORE_BLOCKS.add(Blocks.LAPIS_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_LAPIS_ORE);
        ORE_BLOCKS.add(Blocks.DIAMOND_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        ORE_BLOCKS.add(Blocks.EMERALD_ORE);
        ORE_BLOCKS.add(Blocks.DEEPSLATE_EMERALD_ORE);
        ORE_BLOCKS.add(Blocks.NETHER_QUARTZ_ORE);
        ORE_BLOCKS.add(Blocks.ANCIENT_DEBRIS);
        
        // Log blocks for lumberjack
        LOG_BLOCKS.add(Blocks.OAK_LOG);
        LOG_BLOCKS.add(Blocks.SPRUCE_LOG);
        LOG_BLOCKS.add(Blocks.BIRCH_LOG);
        LOG_BLOCKS.add(Blocks.JUNGLE_LOG);
        LOG_BLOCKS.add(Blocks.ACACIA_LOG);
        LOG_BLOCKS.add(Blocks.DARK_OAK_LOG);
        LOG_BLOCKS.add(Blocks.MANGROVE_LOG);
        LOG_BLOCKS.add(Blocks.CHERRY_LOG);
        LOG_BLOCKS.add(Blocks.CRIMSON_STEM);
        LOG_BLOCKS.add(Blocks.WARPED_STEM);
        LOG_BLOCKS.add(Blocks.OAK_WOOD);
        LOG_BLOCKS.add(Blocks.SPRUCE_WOOD);
        LOG_BLOCKS.add(Blocks.BIRCH_WOOD);
        LOG_BLOCKS.add(Blocks.JUNGLE_WOOD);
        LOG_BLOCKS.add(Blocks.ACACIA_WOOD);
        LOG_BLOCKS.add(Blocks.DARK_OAK_WOOD);
        LOG_BLOCKS.add(Blocks.MANGROVE_WOOD);
        LOG_BLOCKS.add(Blocks.CHERRY_WOOD);
        LOG_BLOCKS.add(Blocks.CRIMSON_HYPHAE);
        LOG_BLOCKS.add(Blocks.WARPED_HYPHAE);
        
        // Block values (money per block)
        BLOCK_VALUES.put(Blocks.COAL_ORE, 3);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_COAL_ORE, 4);
        BLOCK_VALUES.put(Blocks.IRON_ORE, 5);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_IRON_ORE, 6);
        BLOCK_VALUES.put(Blocks.COPPER_ORE, 4);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_COPPER_ORE, 5);
        BLOCK_VALUES.put(Blocks.GOLD_ORE, 10);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_GOLD_ORE, 12);
        BLOCK_VALUES.put(Blocks.NETHER_GOLD_ORE, 8);
        BLOCK_VALUES.put(Blocks.REDSTONE_ORE, 8);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_REDSTONE_ORE, 10);
        BLOCK_VALUES.put(Blocks.LAPIS_ORE, 12);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_LAPIS_ORE, 15);
        BLOCK_VALUES.put(Blocks.DIAMOND_ORE, 50);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_DIAMOND_ORE, 60);
        BLOCK_VALUES.put(Blocks.EMERALD_ORE, 75);
        BLOCK_VALUES.put(Blocks.DEEPSLATE_EMERALD_ORE, 90);
        BLOCK_VALUES.put(Blocks.NETHER_QUARTZ_ORE, 6);
        BLOCK_VALUES.put(Blocks.ANCIENT_DEBRIS, 200);
        
        // Log values
        for (Block log : LOG_BLOCKS) {
            BLOCK_VALUES.put(log, 2);
        }
    }
    
    public static void register() {
        // Register block break event
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            if (isProcessing) return true; // Prevent recursion
            
            // Check if player is sneaking (holding shift)
            if (!player.isShiftKeyDown()) return true;
            
            Block block = state.getBlock();
            
            // Check if vein mining should trigger
            if (ORE_BLOCKS.contains(block)) {
                processVeinMining(serverPlayer, pos, block);
            } else if (LOG_BLOCKS.contains(block)) {
                processLumberjack(serverPlayer, pos, block);
            }
            
            return true; // Allow original block break to continue
        });
        
        AioMod.LOGGER.info("Vein Mining Manager registered.");
    }
    
    private static void processVeinMining(ServerPlayer player, BlockPos startPos, Block oreBlock) {
        ServerLevel level = (ServerLevel) player.level();
        ItemStack tool = player.getMainHandItem();
        
        // Find all connected ore blocks
        Set<BlockPos> vein = findConnectedBlocks(level, startPos, oreBlock, MAX_VEIN_SIZE);
        
        if (vein.size() <= 1) return; // Only the original block, no vein
        
        isProcessing = true;
        
        int blocksbroken = 0;
        long totalMoney = 0;
        int totalXp = 0;
        
        for (BlockPos pos : vein) {
            if (pos.equals(startPos)) continue; // Skip the original block (it will break normally)
            
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != oreBlock) continue;
            
            // Check tool durability
            if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) {
                break; // Tool would break, stop mining
            }
            
            // Break the block
            level.destroyBlock(pos, true, player);
            blocksbroken++;
            
            // Damage tool
            if (tool.isDamageableItem()) {
                tool.hurtAndBreak(1, player, player.getEquipmentSlotForItem(tool));
            }
            
            // Calculate rewards
            int value = BLOCK_VALUES.getOrDefault(oreBlock, 2);
            totalMoney += value;
            totalXp += 5; // XP per ore
        }
        
        // Award XP to Mining skill
        if (totalXp > 0) {
            SkillsManager.addSkillXp(player, com.baesp.aio.rpg.SkillsData.SKILL_MINING, totalXp);
        }
        
        // Award money
        if (totalMoney > 0) {
            EconomyManager.deposit(player, totalMoney);
        }
        
        // Notify player
        if (blocksbroken > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Vein Mining] §fBroke §e" + (blocksbroken + 1) + " §fores! §a+$" + totalMoney + " §b+" + totalXp + " XP"
            ));
        }
        
        isProcessing = false;
    }
    
    private static void processLumberjack(ServerPlayer player, BlockPos startPos, Block logBlock) {
        ServerLevel level = (ServerLevel) player.level();
        ItemStack tool = player.getMainHandItem();
        
        // Find all connected logs (going up primarily for trees)
        Set<BlockPos> tree = findTreeLogs(level, startPos, logBlock, MAX_TREE_SIZE);
        
        if (tree.size() <= 1) return; // Only the original block, no tree
        
        isProcessing = true;
        
        int blocksbroken = 0;
        long totalMoney = 0;
        int totalXp = 0;
        
        // Sort by Y descending so top blocks fall first
        List<BlockPos> sortedLogs = new ArrayList<>(tree);
        sortedLogs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        
        for (BlockPos pos : sortedLogs) {
            if (pos.equals(startPos)) continue; // Skip the original block
            
            BlockState state = level.getBlockState(pos);
            if (!LOG_BLOCKS.contains(state.getBlock())) continue;
            
            // Check tool durability
            if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) {
                break;
            }
            
            // Break the block
            level.destroyBlock(pos, true, player);
            blocksbroken++;
            
            // Damage tool
            if (tool.isDamageableItem()) {
                tool.hurtAndBreak(1, player, player.getEquipmentSlotForItem(tool));
            }
            
            // Calculate rewards
            int value = BLOCK_VALUES.getOrDefault(state.getBlock(), 2);
            totalMoney += value;
            totalXp += 3; // XP per log
        }
        
        // Award XP to Woodcutting skill
        if (totalXp > 0) {
            SkillsManager.addSkillXp(player, com.baesp.aio.rpg.SkillsData.SKILL_WOODCUTTING, totalXp);
        }
        
        // Award money
        if (totalMoney > 0) {
            EconomyManager.deposit(player, totalMoney);
        }
        
        // Notify player
        if (blocksbroken > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Lumberjack] §fFelled §e" + (blocksbroken + 1) + " §flogs! §a+$" + totalMoney + " §b+" + totalXp + " XP"
            ));
        }
        
        isProcessing = false;
    }
    
    private static Set<BlockPos> findConnectedBlocks(ServerLevel level, BlockPos start, Block targetBlock, int maxSize) {
        Set<BlockPos> found = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        
        toCheck.add(start);
        found.add(start);
        
        while (!toCheck.isEmpty() && found.size() < maxSize) {
            BlockPos current = toCheck.poll();
            
            // Check all 6 adjacent blocks
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                
                if (found.contains(neighbor)) continue;
                
                BlockState state = level.getBlockState(neighbor);
                if (state.getBlock() == targetBlock) {
                    found.add(neighbor);
                    toCheck.add(neighbor);
                }
            }
        }
        
        return found;
    }
    
    private static Set<BlockPos> findTreeLogs(ServerLevel level, BlockPos start, Block logBlock, int maxSize) {
        Set<BlockPos> found = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        
        toCheck.add(start);
        found.add(start);
        
        // For trees, we also check diagonals and prioritize going up
        int[] dx = {0, 1, -1, 0, 0, 1, -1, 1, -1};
        int[] dz = {0, 0, 0, 1, -1, 1, 1, -1, -1};
        int[] dy = {1, 0, 0, 0, 0, 0, 0, 0, 0}; // Prioritize upward
        
        while (!toCheck.isEmpty() && found.size() < maxSize) {
            BlockPos current = toCheck.poll();
            
            // Check upward first
            BlockPos up = current.above();
            if (!found.contains(up)) {
                BlockState state = level.getBlockState(up);
                if (LOG_BLOCKS.contains(state.getBlock())) {
                    found.add(up);
                    toCheck.add(up);
                }
            }
            
            // Check horizontal and diagonal neighbors
            for (int i = 1; i < dx.length; i++) {
                BlockPos neighbor = current.offset(dx[i], 0, dz[i]);
                
                if (found.contains(neighbor)) continue;
                
                BlockState state = level.getBlockState(neighbor);
                if (LOG_BLOCKS.contains(state.getBlock())) {
                    found.add(neighbor);
                    toCheck.add(neighbor);
                }
                
                // Also check one block up from diagonal
                BlockPos diagUp = neighbor.above();
                if (!found.contains(diagUp)) {
                    state = level.getBlockState(diagUp);
                    if (LOG_BLOCKS.contains(state.getBlock())) {
                        found.add(diagUp);
                        toCheck.add(diagUp);
                    }
                }
            }
        }
        
        return found;
    }
    
    /**
     * Check if a block is a supported ore for vein mining
     */
    public static boolean isOre(Block block) {
        return ORE_BLOCKS.contains(block);
    }
    
    /**
     * Check if a block is a supported log for lumberjack
     */
    public static boolean isLog(Block block) {
        return LOG_BLOCKS.contains(block);
    }
}
