package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {
    
    @Shadow
    private int[] cookingProgress;
    
    @Shadow
    private int[] cookingTime;
    
    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void aio$onCookTick(Level level, BlockPos pos, BlockState state, 
                                        CampfireBlockEntity blockEntity, CallbackInfo ci) {
        if (!AioMod.CONFIG.fastSmeltEnabled) return;
        
        CampfireBlockEntityMixin mixin = (CampfireBlockEntityMixin) (Object) blockEntity;
        
        // Fast cook all items
        for (int i = 0; i < mixin.cookingProgress.length; i++) {
            if (mixin.cookingProgress[i] > 0 && mixin.cookingTime[i] > 0) {
                mixin.cookingProgress[i] = mixin.cookingTime[i] - 1;
            }
        }
    }
}
