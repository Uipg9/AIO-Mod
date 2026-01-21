package com.baesp.aio.mixin;

import com.baesp.aio.AioMod;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlastFurnaceBlockEntity.class)
public class BlastFurnaceBlockEntityMixin {
    // Inherits fast smelt from AbstractFurnaceBlockEntityMixin
}
