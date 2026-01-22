package com.baesp.aio.features;

import com.baesp.aio.AioMod;
import com.baesp.aio.client.gui.ClientFeatureState;
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
 * Only swaps when player STARTS BREAKING (holding left click), not when just looking
 */
public class AutoToolSwitchManager {
    
    private static BlockState lastSwappedForBlock = null;
    private static boolean wasBreaking = false;
    
    public static void register() {
        // Client-side tick event - check if player is BREAKING a block
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                // Check if feature is enabled
                if (!ClientFeatureState.autoToolSwapEnabled) {
                    return;
                }
                
                if (client.player == null || client.level == null) {
                    return;
                }
                
                // Check if player is holding left-click (breaking)
                boolean isBreaking = client.options.keyAttack.isDown();
                
                // Only trigger on the START of breaking (transition from not-breaking to breaking)
                // OR if we're breaking a different block than before
                if (!isBreaking) {
                    wasBreaking = false;
                    lastSwappedForBlock = null;
                    return;
                }
                
                // Get what the player is looking at
                HitResult hitResult = client.hitResult;
                if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
                    return;
                }
                
                BlockState targetState = client.level.getBlockState(blockHit.getBlockPos());
                
                // Skip air blocks
                if (targetState.isAir()) {
                    return;
                }
                
                // Skip farmland and tillable blocks - don't swap to shovel when farming
                String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).getPath();
                if (blockName.contains("farmland") || blockName.contains("dirt") || 
                    blockName.contains("grass") || blockName.contains("coarse") ||
                    targetState.getBlock() instanceof net.minecraft.world.level.block.CropBlock ||
                    targetState.getBlock() instanceof net.minecraft.world.level.block.StemBlock) {
                    return; // Don't auto-switch for farming-related blocks
                }
                
                // Only swap if we just started breaking OR targeting a different block
                if (wasBreaking && targetState.equals(lastSwappedForBlock)) {
                    return;  // Already swapped for this block
                }
                
                wasBreaking = true;
                lastSwappedForBlock = targetState;
                
                // Check if we need to swap tools
                LocalPlayer player = client.player;
                Inventory inventory = player.getInventory();
                ItemStack currentTool = player.getMainHandItem();
                
                int bestSlot = findBestToolSlot(player, targetState);
                
                // Get current slot through the container slot system
                int currentSlot = inventory.findSlotMatchingItem(currentTool);
                if (currentSlot < 0 || currentSlot > 8) {
                    // Find current selected slot by checking which hotbar slot matches main hand
                    for (int i = 0; i < 9; i++) {
                        if (ItemStack.matches(inventory.getItem(i), currentTool)) {
                            currentSlot = i;
                            break;
                        }
                    }
                }
                
                // If a better tool exists in a different slot, swap to it
                if (bestSlot >= 0 && bestSlot != currentSlot) {
                    ItemStack bestTool = inventory.getItem(bestSlot);
                    // Make sure the better tool is actually better
                    float currentSpeed = currentTool.getDestroySpeed(targetState);
                    float bestSpeed = bestTool.getDestroySpeed(targetState);
                    
                    if (bestSpeed > currentSpeed) {
                        // Swap hotbar slot directly through packet simulation
                        // This is the client-side way to change selected slot
                        inventory.pickSlot(bestSlot);
                        player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§a⚒ Auto-switched to better tool!"),
                            true // Show as action bar
                        );
                    }
                }
            } catch (Exception e) {
                // Silently ignore any errors to prevent crashes
            }
        });
        
        AioMod.LOGGER.info("Auto Tool Switch Manager registered.");
    }
    
    private static int findBestToolSlot(LocalPlayer player, BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0f; // Minimum threshold (hand speed)
        
        // Check hotbar slots only (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                float speed = stack.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        
        return bestSlot;
    }
}
