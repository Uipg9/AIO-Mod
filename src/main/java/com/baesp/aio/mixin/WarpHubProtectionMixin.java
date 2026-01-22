package com.baesp.aio.mixin;

import com.baesp.aio.warp.WarpManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent block breaking in the Warp Hub dimension
 * This stops players from mining the gold/diamond blocks and exploiting them
 */
@Mixin(ServerPlayerGameMode.class)
public class WarpHubProtectionMixin {
    
    @Shadow
    protected ServerPlayer player;
    
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player != null && WarpManager.isInWarpHub(player)) {
            // Cancel block breaking in warp hub
            cir.setReturnValue(false);
        }
    }
}
