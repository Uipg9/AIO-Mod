package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SmokerBlockEntity.class)
public class SmokerBlockEntityMixin {
    // Inherits fast smelt from AbstractFurnaceBlockEntityMixin
}
