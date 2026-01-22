package com.baesp.aio.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for Villager to invoke protected methods.
 * Used by trade cycling to update special prices after cycling.
 */
@Mixin(Villager.class)
public interface VillagerAccessor {

    @Invoker("updateSpecialPrices")
    void invokeUpdateSpecialPrices(Player player);

}
