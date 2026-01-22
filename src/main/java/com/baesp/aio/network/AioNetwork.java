package com.baesp.aio.network;

import com.baesp.aio.AioMod;
import com.baesp.aio.ascendancy.AscendancyManager;
import com.baesp.aio.rpg.economy.EconomyManager;
import com.baesp.aio.rpg.SkillsManager;
import com.baesp.aio.squat.SquatGrowManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class AioNetwork {
    
    // Packet IDs
    public static final Identifier REQUEST_DATA_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "request_data");
    public static final Identifier SYNC_DATA_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "sync_data");
    public static final Identifier TOGGLE_SQUAT_GROW_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "toggle_squat_grow");
    public static final Identifier TOGGLE_VOID_MAGNET_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "toggle_void_magnet");
    public static final Identifier BUY_ITEM_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "buy_item");
    public static final Identifier BUY_UPGRADE_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "buy_upgrade");
    public static final Identifier ASCEND_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "ascend");
    public static final Identifier CYCLE_TRADES_ID = Identifier.fromNamespaceAndPath(AioMod.MOD_ID, "cycle_trades");
    
    // Request data packet (C2S)
    public record RequestDataPacket() implements CustomPacketPayload {
        public static final Type<RequestDataPacket> TYPE = new Type<>(REQUEST_DATA_ID);
        public static final StreamCodec<FriendlyByteBuf, RequestDataPacket> CODEC = StreamCodec.unit(new RequestDataPacket());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Sync data packet (S2C) - sends all player data
    public record SyncDataPacket(
        int soulLevel, int soulXp, int soulXpToNextLevel, int ascensionCount, int prestigePoints,
        int[] skillLevels, int[] skillXp,
        long money,
        int[] upgradeLevels,
        boolean squatGrowEnabled
    ) implements CustomPacketPayload {
        public static final Type<SyncDataPacket> TYPE = new Type<>(SYNC_DATA_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncDataPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.soulLevel);
                buf.writeInt(packet.soulXp);
                buf.writeInt(packet.soulXpToNextLevel);
                buf.writeInt(packet.ascensionCount);
                buf.writeInt(packet.prestigePoints);
                buf.writeInt(packet.skillLevels.length);
                for (int level : packet.skillLevels) buf.writeInt(level);
                for (int xp : packet.skillXp) buf.writeInt(xp);
                buf.writeLong(packet.money);
                buf.writeInt(packet.upgradeLevels.length);
                for (int level : packet.upgradeLevels) buf.writeInt(level);
                buf.writeBoolean(packet.squatGrowEnabled);
            },
            buf -> {
                int soulLevel = buf.readInt();
                int soulXp = buf.readInt();
                int soulXpToNextLevel = buf.readInt();
                int ascensionCount = buf.readInt();
                int prestigePoints = buf.readInt();
                int skillCount = buf.readInt();
                int[] skillLevels = new int[skillCount];
                int[] skillXp = new int[skillCount];
                for (int i = 0; i < skillCount; i++) skillLevels[i] = buf.readInt();
                for (int i = 0; i < skillCount; i++) skillXp[i] = buf.readInt();
                long money = buf.readLong();
                int upgradeCount = buf.readInt();
                int[] upgradeLevels = new int[upgradeCount];
                for (int i = 0; i < upgradeCount; i++) upgradeLevels[i] = buf.readInt();
                boolean squatGrowEnabled = buf.readBoolean();
                return new SyncDataPacket(soulLevel, soulXp, soulXpToNextLevel, ascensionCount, prestigePoints,
                    skillLevels, skillXp, money, upgradeLevels, squatGrowEnabled);
            }
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Toggle squat grow packet (C2S)
    public record ToggleSquatGrowPacket() implements CustomPacketPayload {
        public static final Type<ToggleSquatGrowPacket> TYPE = new Type<>(TOGGLE_SQUAT_GROW_ID);
        public static final StreamCodec<FriendlyByteBuf, ToggleSquatGrowPacket> CODEC = StreamCodec.unit(new ToggleSquatGrowPacket());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Toggle void magnet packet (C2S)
    public record ToggleVoidMagnetPacket(boolean enabled) implements CustomPacketPayload {
        public static final Type<ToggleVoidMagnetPacket> TYPE = new Type<>(TOGGLE_VOID_MAGNET_ID);
        public static final StreamCodec<FriendlyByteBuf, ToggleVoidMagnetPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.enabled),
            buf -> new ToggleVoidMagnetPacket(buf.readBoolean())
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Buy item packet (C2S)
    public record BuyItemPacket(int category, int itemIndex) implements CustomPacketPayload {
        public static final Type<BuyItemPacket> TYPE = new Type<>(BUY_ITEM_ID);
        public static final StreamCodec<FriendlyByteBuf, BuyItemPacket> CODEC = StreamCodec.of(
            (buf, packet) -> { buf.writeInt(packet.category); buf.writeInt(packet.itemIndex); },
            buf -> new BuyItemPacket(buf.readInt(), buf.readInt())
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Buy upgrade packet (C2S)
    public record BuyUpgradePacket(int upgradeIndex) implements CustomPacketPayload {
        public static final Type<BuyUpgradePacket> TYPE = new Type<>(BUY_UPGRADE_ID);
        public static final StreamCodec<FriendlyByteBuf, BuyUpgradePacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeInt(packet.upgradeIndex),
            buf -> new BuyUpgradePacket(buf.readInt())
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Ascend packet (C2S)
    public record AscendPacket() implements CustomPacketPayload {
        public static final Type<AscendPacket> TYPE = new Type<>(ASCEND_ID);
        public static final StreamCodec<FriendlyByteBuf, AscendPacket> CODEC = StreamCodec.unit(new AscendPacket());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // Cycle trades packet (C2S)
    public record CycleTradesPacket() implements CustomPacketPayload {
        public static final Type<CycleTradesPacket> TYPE = new Type<>(CYCLE_TRADES_ID);
        public static final StreamCodec<FriendlyByteBuf, CycleTradesPacket> CODEC = StreamCodec.unit(new CycleTradesPacket());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    public static void registerServer() {
        // Register C2S packet types
        PayloadTypeRegistry.playC2S().register(RequestDataPacket.TYPE, RequestDataPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleSquatGrowPacket.TYPE, ToggleSquatGrowPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleVoidMagnetPacket.TYPE, ToggleVoidMagnetPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyItemPacket.TYPE, BuyItemPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyUpgradePacket.TYPE, BuyUpgradePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AscendPacket.TYPE, AscendPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CycleTradesPacket.TYPE, CycleTradesPacket.CODEC);
        
        // Register S2C packet types
        PayloadTypeRegistry.playS2C().register(SyncDataPacket.TYPE, SyncDataPacket.CODEC);
        
        // Handle data request
        ServerPlayNetworking.registerGlobalReceiver(RequestDataPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> sendSyncData(player));
        });
        
        // Handle squat grow toggle
        ServerPlayNetworking.registerGlobalReceiver(ToggleSquatGrowPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                SquatGrowManager.toggleSquatGrow(player);
                boolean enabled = SquatGrowManager.isSquatGrowEnabled(player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    enabled ? "§aSquat Grow enabled!" : "§cSquat Grow disabled!"
                ));
            });
        });
        
        // Handle void magnet toggle
        ServerPlayNetworking.registerGlobalReceiver(ToggleVoidMagnetPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                // Void magnet state is stored on the server
                com.baesp.aio.features.VoidMagnetManager.setEnabled(player, packet.enabled());
            });
        });
        
        // Handle buy upgrade
        ServerPlayNetworking.registerGlobalReceiver(BuyUpgradePacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                AscendancyManager.buyUpgrade(player, packet.upgradeIndex);
                sendSyncData(player);
            });
        });
        
        // Handle ascend
        ServerPlayNetworking.registerGlobalReceiver(AscendPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                AscendancyManager.ascend(player);
                sendSyncData(player);
            });
        });
        
        // Handle buy item
        ServerPlayNetworking.registerGlobalReceiver(BuyItemPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                // Use ShopManager to process purchase
                com.baesp.aio.rpg.economy.ShopManager.buyItem(player, packet.category(), packet.itemIndex());
            });
        });
        
        // Handle trade cycling
        ServerPlayNetworking.registerGlobalReceiver(CycleTradesPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                com.baesp.aio.features.TradeCyclingManager.cycleTrades(player);
            });
        });
        
        AioMod.LOGGER.info("AIO Network (Server) registered.");
    }
    
    public static void sendSyncData(ServerPlayer player) {
        // Gather all player data
        int soulLevel = AscendancyManager.getSoulLevel(player);
        int soulXp = AscendancyManager.getSoulXp(player);
        int soulXpToNextLevel = AscendancyManager.getXpForNextLevel(soulLevel);
        int ascensionCount = AscendancyManager.getAscensionCount(player);
        int prestigePoints = AscendancyManager.getPrestigePoints(player);
        
        // Skills
        int[] skillLevels = new int[6];
        int[] skillXp = new int[6];
        for (int i = 0; i < 6; i++) {
            skillLevels[i] = SkillsManager.getSkillLevel(player, i);
            skillXp[i] = SkillsManager.getSkillXp(player, i);
        }
        
        // Economy
        long money = EconomyManager.getMoney(player);
        
        // Upgrades
        int[] upgradeLevels = new int[9];
        for (int i = 0; i < 9; i++) {
            upgradeLevels[i] = AscendancyManager.getUpgradeLevel(player, i);
        }
        
        // Squat grow
        boolean squatGrowEnabled = SquatGrowManager.isSquatGrowEnabled(player);
        
        // Send packet
        SyncDataPacket packet = new SyncDataPacket(
            soulLevel, soulXp, soulXpToNextLevel, ascensionCount, prestigePoints,
            skillLevels, skillXp, money, upgradeLevels, squatGrowEnabled
        );
        
        ServerPlayNetworking.send(player, packet);
    }
}
