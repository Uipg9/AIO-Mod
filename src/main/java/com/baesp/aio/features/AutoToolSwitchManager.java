package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Auto Tool Switch - Automatically finds and switches to best tool for breaking blocks
 * Uses client-side targeting detection to swap tools BEFORE player starts breaking
 */
public class AutoToolSwitchManager {
    
    private static BlockState lastTargetedBlock = null;
    
    public static void register() {
        // Client-side tick event - check what block player is targeting
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }
            
            // Get what the player is looking at
            HitResult hitResult = client.hitResult;
            if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockState targetState = client.level.getBlockState(blockHit.getBlockPos());
                
                // Only check if targeting a different block
                if (!targetState.equals(lastTargetedBlock)) {
                    lastTargetedBlock = targetState;
                    
                    // Check if we need to swap tools
                    LocalPlayer player = client.player;
                    Inventory inventory = player.getInventory();
                    ItemStack currentTool = player.getMainHandItem();
                    ItemStack bestTool = findBestToolClient(player, targetState);
                    
                    // If player is using wrong tool, auto-switch to better tool in hotbar
                    if (bestTool != null && !ItemStack.matches(currentTool, bestTool)) {
                        int bestSlot = findToolInHotbarClient(player, bestTool);
                        
                        // Use reflection to access/modify the selected slot
                        try {
                            java.lang.reflect.Field selectedField = Inventory.class.getDeclaredField("selected");
                            selectedField.setAccessible(true);
                            int currentSlot = selectedField.getInt(inventory);
                            
                            if (bestSlot >= 0 && bestSlot != currentSlot) {
                                // Swap to the better tool slot
                                selectedField.setInt(inventory, bestSlot);
                                player.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal("§aAuto-switched to better tool!"),
                                    true // Show as action bar
                                );
                            }
                        } catch (Exception e) {
                            AioMod.LOGGER.error("Failed to access selected hotbar slot: " + e.getMessage());
                        }
                    }
                }
            } else {
                // Not looking at a block
                lastTargetedBlock = null;
            }
        });
        
        AioMod.LOGGER.info("Auto Tool Switch Manager registered (client-side targeting detection).");
    }
    
    private static ItemStack findBestToolClient(LocalPlayer player, BlockState state) {
        ItemStack bestTool = null;
        float bestSpeed = 0;
        
        // Check hotbar slots only (0-8)
        for (int i = 0; i < 9; i++) {
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
    
    private static int findToolInHotbarClient(LocalPlayer player, ItemStack targetTool) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == targetTool.getItem()) {
                return i;
            }
        }
        return -1;
    }
}
