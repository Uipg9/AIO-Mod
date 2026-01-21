package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Auto Tool Switch - Automatically finds and suggests best tool for breaking blocks
 * Note: Automatic switching requires client-side implementation
 */
public class AutoToolSwitchManager {
    
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack currentTool = serverPlayer.getMainHandItem();
                ItemStack bestTool = findBestTool(serverPlayer, state);
                
                // If player is using wrong tool, send hint
                if (bestTool != null && !ItemStack.matches(currentTool, bestTool)) {
                    int slot = findToolInHotbar(serverPlayer, bestTool);
                    if (slot >= 0) {
                        // Found better tool in hotbar
                        serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§e⚠ Better tool in slot " + (slot + 1))
                        );
                    }
                }
            }
            return true;
        });
        
        AioMod.LOGGER.info("Auto Tool Switch Manager registered.");
    }
    
    private static ItemStack findBestTool(ServerPlayer player, BlockState state) {
        ItemStack bestTool = null;
        float bestSpeed = 0;
        
        // Check all inventory slots
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                float speed = stack.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestTool = stack;
                }
            }
        }
        
        return bestTool;
    }
    
    private static int findToolInHotbar(ServerPlayer player, ItemStack targetTool) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == targetTool.getItem()) {
                return i;
            }
        }
        return -1;
    }
}
