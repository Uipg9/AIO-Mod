package com.baesp.aio.mixin.client;

import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

/**
 * Mixin to auto-skip the experimental features warning when creating worlds.
 * This is needed because custom dimensions (warp_hub, player_home) are considered
 * experimental features by Minecraft.
 * 
 * The warning screen is a ConfirmScreen with specific title text.
 */
@Mixin(ConfirmScreen.class)
public class ExperimentalWarningScreenMixin extends Screen {
    
    @Shadow @Final
    private BooleanConsumer callback;
    
    @Shadow @Final
    private Component title;
    
    protected ExperimentalWarningScreenMixin(Component title) {
        super(title);
    }
    
    /**
     * Auto-confirm if this is the experimental features warning.
     * We check for the specific title text that Minecraft uses.
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void aio_autoConfirmExperimental(CallbackInfo ci) {
        // Check if this is the experimental warning screen
        String titleStr = this.title.getString().toLowerCase();
        if (titleStr.contains("experimental") || titleStr.contains("warning")) {
            // Auto-confirm and skip the screen
            this.callback.accept(true);
            ci.cancel();
        }
    }
}
