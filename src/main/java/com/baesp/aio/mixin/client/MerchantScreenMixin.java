package com.baesp.aio.mixin.client;

import com.baesp.aio.network.AioNetworkClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a trade cycling button to the villager trading screen.
 * This button will cycle the villager's trades by resetting them.
 * Only works on Novice-level villagers (no trades made yet).
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends Screen {
    
    protected MerchantScreenMixin(Component title) {
        super(title);
    }
    
    @Inject(method = "init", at = @At("TAIL"))
    private void aio_addTradeCyclingButton(CallbackInfo ci) {
        // Position button to top-left of trading UI (near villager portrait)
        // The trading screen is 276x166 centered on screen
        int guiLeft = (this.width - 276) / 2;
        int guiTop = (this.height - 166) / 2;
        
        // Place button next to the villager's portrait area (top-left of UI)
        int buttonX = guiLeft + 4;
        int buttonY = guiTop + 4;
        
        // Create cycle trades button - small and unobtrusive
        Button cycleButton = Button.builder(
            Component.literal("§a⟳"),
            button -> {
                // Send packet to server to cycle trades - silent operation
                AioNetworkClient.sendCycleTrades();
            }
        ).bounds(buttonX, buttonY, 18, 18).build();
        
        // Add tooltip
        cycleButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.literal("§aCycle Trades\n§7Click to reset villager trades\n§c⚠ Only works on Novice villagers!")
        ));
        
        this.addRenderableWidget(cycleButton);
        
        // Info button beside the cycle button
        Button infoButton = Button.builder(
            Component.literal("§e?"),
            button -> {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6Trade Cycling: §7Click ⟳ to cycle trades. Only works on Novice villagers!"), false);
                }
            }
        ).bounds(buttonX + 20, buttonY, 18, 18).build();
        
        infoButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.literal("§eInfo")
        ));
        
        this.addRenderableWidget(infoButton);
    }
}
