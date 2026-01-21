package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    
    @Shadow
    int cookingProgress;
    
    @Shadow
    int cookingTotalTime;
    
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void aio$onTick(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, 
                                    net.minecraft.world.level.block.state.BlockState state, 
                                    AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!AioMod.CONFIG.fastSmeltEnabled) return;
        
        // Access the mixin target's fields
        AbstractFurnaceBlockEntityMixin mixin = (AbstractFurnaceBlockEntityMixin) (Object) blockEntity;
        
        // If cooking, set progress to almost complete (1 tick from done)
        if (mixin.cookingProgress > 0 && mixin.cookingTotalTime > 0) {
            mixin.cookingProgress = mixin.cookingTotalTime - 1;
        }
    }
}
