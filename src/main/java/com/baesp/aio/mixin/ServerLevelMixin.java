package com.baesp.aio.mixin;

import com.baesp.aio.villagespawn.VillageSpawnManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    
    @Shadow
    public abstract ServerLevelData serverLevelData();
    
    @Inject(method = "setDefaultSpawnPos", at = @At("HEAD"), cancellable = true)
    private void aio$onSetDefaultSpawnPos(net.minecraft.core.BlockPos pos, float angle, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        
        // Only modify spawn for overworld on new world creation
        if (level.dimension().equals(ServerLevel.OVERWORLD)) {
            if (VillageSpawnManager.setVillageSpawn(level, serverLevelData())) {
                ci.cancel(); // Cancel vanilla spawn setting if we set village spawn
            }
        }
    }
}
