package com.baesp.aio.mixin;

import com.baesp.aio.villagespawn.VillageSpawnManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    
    @Shadow
    public abstract ServerLevelData serverLevelData();
    
    @ModifyVariable(method = "setDefaultSpawnPos", at = @At("HEAD"), argsOnly = true)
    private BlockPos aio$modifySpawnPos(BlockPos originalPos) {
        ServerLevel level = (ServerLevel) (Object) this;
        
        // Only modify spawn for overworld on new world creation
        if (level.dimension().equals(ServerLevel.OVERWORLD)) {
            BlockPos villagePos = VillageSpawnManager.findVillageSpawn(level, serverLevelData());
            if (villagePos != null) {
                return villagePos; // Use village position instead
            }
        }
        
        return originalPos; // Use vanilla position
    }
}
