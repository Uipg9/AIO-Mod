package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
    
    @Shadow
    private int cooldownTime;
    
    @Inject(method = "setCooldown", at = @At("HEAD"), cancellable = true)
    private void aio$setCooldown(int cooldown, CallbackInfo ci) {
        if (AioMod.CONFIG.fastHoppersEnabled) {
            this.cooldownTime = AioMod.CONFIG.hopperCooldown;
            ci.cancel();
        }
    }
}
