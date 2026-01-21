package com.baesp.aio.mixin;

import com.baesp.aio.squat.SquatGrowManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
    @Unique
    private boolean aio$wasCrouchingLastTick = false;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void aio$onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        
        if (player instanceof ServerPlayer serverPlayer) {
            boolean isCrouching = player.isCrouching();
            boolean onGround = player.onGround();
            
            // Only trigger on crouch start (not while holding)
            if (onGround && !aio$wasCrouchingLastTick && isCrouching) {
                SquatGrowManager.performSquatGrow(serverPlayer);
            }
            
            aio$wasCrouchingLastTick = isCrouching;
        }
    }
}
