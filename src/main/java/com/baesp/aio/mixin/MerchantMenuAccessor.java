package com.baesp.aio.mixin;

import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for MerchantMenu to get the underlying trader.
 * Used by trade cycling to access the villager being traded with.
 */
@Mixin(MerchantMenu.class)
public interface MerchantMenuAccessor {

    @Accessor("trader")
    Merchant getTrader();

    @Accessor("tradeContainer")
    MerchantContainer getTradeContainer();

}
