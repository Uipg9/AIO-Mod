package com.baesp.aio.mixin;

import com.baesp.aio.rpg.SkillsData;
import com.baesp.aio.rpg.SkillsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = false)
    private void aio$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        
        // Give defense XP when taking damage
        if (amount > 0) {
            SkillsManager.addSkillXp(player, SkillsData.SKILL_DEFENSE, (int) Math.max(1, amount / 2));
        }
    }
}
