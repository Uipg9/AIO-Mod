package com.baesp.aio.network;

import com.baesp.aio.AioMod;
import com.baesp.aio.client.gui.ClientDataCache;
import com.baesp.aio.client.hud.HudRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AioNetworkClient {
    
    public static void register() {
        // Register client-side receiver for sync data
        ClientPlayNetworking.registerGlobalReceiver(AioNetwork.SyncDataPacket.TYPE, (packet, context) -> {
            // Update client cache on main thread
            context.client().execute(() -> {
                ClientDataCache cache = ClientDataCache.get();
                
                // Check for changes to trigger floating messages
                long oldMoney = cache.money;
                int oldSoulXp = cache.soulXp;
                
                cache.updateFromPacket(packet);
                
                // Floating messages for changes
                if (packet.money() != oldMoney) {
                    long diff = packet.money() - oldMoney;
                    HudRenderer.addMoneyMessage(diff);
                }
                if (packet.soulXp() != oldSoulXp) {
                    int diff = packet.soulXp() - oldSoulXp;
                    HudRenderer.addSoulXpMessage(diff);
                }
                
                AioMod.LOGGER.debug("Client data synced from server");
            });
        });
        
        AioMod.LOGGER.info("AIO Network (Client) registered.");
    }
    
    public static void sendRequestData() {
        if (ClientPlayNetworking.canSend(AioNetwork.RequestDataPacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.RequestDataPacket());
        }
    }
    
    public static void sendToggleSquatGrow() {
        if (ClientPlayNetworking.canSend(AioNetwork.ToggleSquatGrowPacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.ToggleSquatGrowPacket());
        }
    }
    
    public static void sendToggleVoidMagnet(boolean enabled) {
        if (ClientPlayNetworking.canSend(AioNetwork.ToggleVoidMagnetPacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.ToggleVoidMagnetPacket(enabled));
        }
    }
    
    public static void sendBuyUpgrade(int upgradeIndex) {
        if (ClientPlayNetworking.canSend(AioNetwork.BuyUpgradePacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.BuyUpgradePacket(upgradeIndex));
        }
    }
    
    public static void sendAscend() {
        if (ClientPlayNetworking.canSend(AioNetwork.AscendPacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.AscendPacket());
        }
    }
    
    public static void sendBuyItem(int category, int itemIndex) {
        if (ClientPlayNetworking.canSend(AioNetwork.BuyItemPacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.BuyItemPacket(category, itemIndex));
        }
    }
    
    public static void sendCycleTrades() {
        if (ClientPlayNetworking.canSend(AioNetwork.CycleTradesPacket.TYPE)) {
            ClientPlayNetworking.send(new AioNetwork.CycleTradesPacket());
        }
    }
}
