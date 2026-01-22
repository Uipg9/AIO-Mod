package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent crop trampling when players jump on farmland.
 * The fallOn method is called when an entity lands on the block.
 */
@Mixin(FarmBlock.class)
public class FarmBlockMixin {
    
    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void aio$preventTrampling(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        // Only prevent if config is enabled
        if (AioMod.CONFIG.noCropTrampling) {
            // Cancel the trampling entirely - this prevents farmland from turning to dirt
            ci.cancel();
        }
    }
}
