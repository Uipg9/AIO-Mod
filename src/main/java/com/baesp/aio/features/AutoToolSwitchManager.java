package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Auto Tool Switch - Automatically finds and switches to best tool for breaking blocks
 */
public class AutoToolSwitchManager {
    
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack currentTool = serverPlayer.getMainHandItem();
                ItemStack bestTool = findBestTool(serverPlayer, state);
                
                // If player is using wrong tool, auto-switch to better tool in hotbar
                if (bestTool != null && !ItemStack.matches(currentTool, bestTool)) {
                    int slot = findToolInHotbar(serverPlayer, bestTool);
                    if (slot >= 0) {
                        // Get current selected slot using private access workaround
                        int currentSlot = -1;
                        for (int i = 0; i < 9; i++) {
                            if (serverPlayer.getInventory().getItem(i) == currentTool) {
                                currentSlot = i;
                                break;
                            }
                        }
                        
                        if (currentSlot != slot) {
                            // Swap the items in inventory to simulate tool selection
                            ItemStack toolToUse = serverPlayer.getInventory().getItem(slot);
                            ItemStack oldTool = serverPlayer.getInventory().getItem(currentSlot);
                            serverPlayer.getInventory().setItem(slot, oldTool);
                            serverPlayer.getInventory().setItem(currentSlot, toolToUse);
                            serverPlayer.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("§aAuto-switched to better tool!")
                            );
                        }
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
