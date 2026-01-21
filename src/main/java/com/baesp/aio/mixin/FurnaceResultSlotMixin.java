package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import com.baesp.aio.data.PlayerDataManager;
import com.baesp.aio.rpg.SkillsData;
import com.baesp.aio.rpg.SkillsManager;
import com.baesp.aio.rpg.economy.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {
    
    @Shadow
    @Final
    private Player player;
    
    @Inject(method = "onTake", at = @At("HEAD"))
    private void aio$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            int count = stack.getCount();
            
            // Give smithing XP for smelting
            if (AioMod.CONFIG.skillsEnabled) {
                SkillsManager.addSkillXp(serverPlayer, SkillsData.SKILL_SMITHING, 
                    AioMod.CONFIG.xpPerSkillAction * count);
            }
            
            // Give money for smelting
            if (AioMod.CONFIG.economyEnabled && AioMod.CONFIG.smeltingRewardCoins > 0) {
                EconomyManager.deposit(serverPlayer, (long) AioMod.CONFIG.smeltingRewardCoins * count);
            }
        }
    }
}
