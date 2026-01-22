package com.baesp.aio.commands;

import com.baesp.aio.AioMod;
import com.baesp.aio.ascendancy.AscendancyData;
import com.baesp.aio.ascendancy.AscendancyManager;
import com.baesp.aio.data.PlayerDataManager;
import com.baesp.aio.gui.AscendancyScreen;
import com.baesp.aio.gui.ShopScreen;
import com.baesp.aio.gui.SkillsScreen;
import com.baesp.aio.rpg.SkillsData;
import com.baesp.aio.rpg.economy.EconomyManager;
import com.baesp.aio.squat.SquatGrowManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AioCommands {
    
    private static boolean hasOp(CommandSourceStack source) {
        // Simple permission check - allow in singleplayer or for ops
        try {
            if (source.getServer() == null) return true; // Allow if no server (shouldn't happen but be safe)
            if (source.getServer().isSingleplayer()) return true;
            // For dedicated servers, check entity permissions
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer sp) {
                return !sp.gameMode.isSurvival(); // Creative/spectator = op access
            }
            return false;
        } catch (Exception e) {
            return true; // Default to allowing on error
        }
    }
    
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerMainCommands(dispatcher);
            registerEconomyCommands(dispatcher);
            registerSkillsCommands(dispatcher);
            registerAscendancyCommands(dispatcher);
        });
    }
    
    private static void registerMainCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /aio - Main mod info
        dispatcher.register(Commands.literal("aio")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§6=== All-in-One Mod ==="), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§7Version: §e1.0.0"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§7Use /aio help for commands"), false);
                return 1;
            })
            .then(Commands.literal("help")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§6=== AIO Commands ==="), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/skills §7- Open skills menu"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/ascend §7- Open ascendancy menu"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/shop §7- Open shop menu"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/balance §7- Check your balance"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/pay <player> <amount> §7- Pay another player"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/squatgrow §7- Toggle squat grow"), false);
                    return 1;
                })
            )
            .then(Commands.literal("reload")
                .requires(AioCommands::hasOp)
                .executes(ctx -> {
                    AioMod.CONFIG = com.baesp.aio.config.AioConfig.load();
                    ctx.getSource().sendSuccess(() -> Component.literal("§aAIO config reloaded!"), true);
                    return 1;
                })
            )
        );
        
        // /squatgrow - Toggle squat grow
        dispatcher.register(Commands.literal("squatgrow")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                SquatGrowManager.toggleSquatGrow(player);
                boolean enabled = SquatGrowManager.isSquatGrowEnabled(player);
                ctx.getSource().sendSuccess(() -> 
                    Component.literal(enabled ? "§aSquat Grow enabled!" : "§cSquat Grow disabled!"), false);
                return 1;
            })
        );
    }
    
    private static void registerEconomyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /sell - Sell item in hand
        dispatcher.register(Commands.literal("sell")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
                
                if (heldItem.isEmpty()) {
                    ctx.getSource().sendFailure(Component.literal("§cYou must be holding an item to sell!"));
                    return 0;
                }
                
                // Calculate sell price (roughly 30% of what it would cost to buy)
                long sellPrice = calculateSellPrice(heldItem);
                int count = heldItem.getCount();
                long totalPrice = sellPrice * count;
                
                if (totalPrice <= 0) {
                    ctx.getSource().sendFailure(Component.literal("§cThis item cannot be sold!"));
                    return 0;
                }
                
                // Remove item from hand and give money
                String itemName = heldItem.getHoverName().getString();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                EconomyManager.deposit(player, totalPrice);
                
                ctx.getSource().sendSuccess(() -> 
                    Component.literal("§aSold §f" + count + "x " + itemName + " §afor §e$" + EconomyManager.formatMoney(totalPrice)), false);
                return 1;
            })
            // /sell all - Sell entire inventory
            .then(Commands.literal("all")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    long totalEarned = 0;
                    int itemsSold = 0;
                    
                    // Go through entire inventory
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                        if (stack.isEmpty()) continue;
                        
                        long sellPrice = calculateSellPrice(stack);
                        if (sellPrice > 0) {
                            int count = stack.getCount();
                            long price = sellPrice * count;
                            totalEarned += price;
                            itemsSold += count;
                            player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                        }
                    }
                    
                    if (itemsSold == 0) {
                        ctx.getSource().sendFailure(Component.literal("§cNo sellable items in inventory!"));
                        return 0;
                    }
                    
                    EconomyManager.deposit(player, totalEarned);
                    final long finalEarned = totalEarned;
                    final int finalSold = itemsSold;
                    ctx.getSource().sendSuccess(() -> 
                        Component.literal("§aSold §f" + finalSold + " items §afor §e$" + EconomyManager.formatMoney(finalEarned)), false);
                    return 1;
                })
            )
        );
        
        // /balance (/bal)
        dispatcher.register(Commands.literal("balance")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                long balance = EconomyManager.getMoney(player);
                ctx.getSource().sendSuccess(() -> 
                    Component.literal("§6Balance: §e$" + EconomyManager.formatMoney(balance)), false);
                return 1;
            })
        );
        dispatcher.register(Commands.literal("bal").redirect(dispatcher.getRoot().getChild("balance")));
        
        // /pay <player> <amount>
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                    .executes(ctx -> {
                        ServerPlayer sender = ctx.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        long amount = LongArgumentType.getLong(ctx, "amount");
                        
                        if (sender.equals(target)) {
                            ctx.getSource().sendFailure(Component.literal("§cYou can't pay yourself!"));
                            return 0;
                        }
                        
                        if (!EconomyManager.withdraw(sender, amount)) {
                            ctx.getSource().sendFailure(Component.literal("§cInsufficient funds!"));
                            return 0;
                        }
                        
                        EconomyManager.deposit(target, amount);
                        
                        ctx.getSource().sendSuccess(() -> 
                            Component.literal("§aPaid §e$" + EconomyManager.formatMoney(amount) + " §ato §e" + target.getName().getString()), false);
                        target.sendSystemMessage(
                            Component.literal("§aReceived §e$" + EconomyManager.formatMoney(amount) + " §afrom §e" + sender.getName().getString())
                        );
                        return 1;
                    })
                )
            )
        );
        
        // Admin commands
        // /eco set <player> <amount>
        dispatcher.register(Commands.literal("eco")
            .requires(AioCommands::hasOp)
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            EconomyManager.setMoney(target, amount);
                            ctx.getSource().sendSuccess(() -> 
                                Component.literal("§aSet §e" + target.getName().getString() + "'s §abalance to §e$" + EconomyManager.formatMoney(amount)), true);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            EconomyManager.deposit(target, amount);
                            ctx.getSource().sendSuccess(() -> 
                                Component.literal("§aGave §e$" + EconomyManager.formatMoney(amount) + " §ato §e" + target.getName().getString()), true);
                            return 1;
                        })
                    )
                )
            )
        );
    }
    
    private static void registerSkillsCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Admin: /setskill <player> <skill> <level>
        dispatcher.register(Commands.literal("setskill")
            .requires(AioCommands::hasOp)
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("skill", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest(SkillsData.SKILL_FARMING);
                        builder.suggest(SkillsData.SKILL_COMBAT);
                        builder.suggest(SkillsData.SKILL_DEFENSE);
                        builder.suggest(SkillsData.SKILL_SMITHING);
                        builder.suggest(SkillsData.SKILL_WOODCUTTING);
                        builder.suggest(SkillsData.SKILL_MINING);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String skill = StringArgumentType.getString(ctx, "skill");
                            int level = IntegerArgumentType.getInteger(ctx, "level");
                            
                            SkillsData skills = PlayerDataManager.getData(target).skills;
                            skills.setSkillLevel(skill, level);
                            
                            ctx.getSource().sendSuccess(() -> 
                                Component.literal("§aSet §e" + target.getName().getString() + "'s §a" + skill + " to level §e" + level), true);
                            return 1;
                        })
                    )
                )
            )
        );
    }
    
    private static void registerAscendancyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /ascension - Perform ascension (requires confirmation)
        dispatcher.register(Commands.literal("ascension")
            .then(Commands.literal("confirm")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!AscendancyManager.canAscend(player)) {
                        ctx.getSource().sendFailure(Component.literal("§c✖ You need Soul Level " + AscendancyManager.REQUIRED_SOUL_LEVEL_FOR_ASCENSION + " to ascend!"));
                        return 0;
                    }
                    AscendancyManager.performAscension(player);
                    return 1;
                })
            )
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                if (!AscendancyManager.canAscend(player)) {
                    ctx.getSource().sendFailure(Component.literal("§c✖ You need Soul Level " + AscendancyManager.REQUIRED_SOUL_LEVEL_FOR_ASCENSION + " to ascend!"));
                    return 0;
                }
                ctx.getSource().sendSuccess(() -> Component.literal("§c⚠ WARNING: Ascension will reset your progress!"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§7- Your inventory will be cleared"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§7- Your ender chest will be cleared"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§7- You will be teleported to spawn"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§a+ You will gain Prestige Points"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§eType §6/ascension confirm §eto proceed"), false);
                return 1;
            })
        );
        
        // Admin: /prestige <player> <points>
        dispatcher.register(Commands.literal("prestige")
            .requires(AioCommands::hasOp)
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("points", IntegerArgumentType.integer(0))
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        int points = IntegerArgumentType.getInteger(ctx, "points");
                        
                        AscendancyData data = PlayerDataManager.getData(target).ascendancy;
                        data.prestigePoints = points;
                        
                        ctx.getSource().sendSuccess(() -> 
                            Component.literal("§aSet §e" + target.getName().getString() + "'s §aprestige points to §e" + points), true);
                        return 1;
                    })
                )
            )
        );
        
        // Admin: /soulxp <player> <amount>
        dispatcher.register(Commands.literal("soulxp")
            .requires(AioCommands::hasOp)
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        long amount = LongArgumentType.getLong(ctx, "amount");
                        
                        AscendancyManager.addSoulXp(target, amount);
                        
                        ctx.getSource().sendSuccess(() -> 
                            Component.literal("§aGave §e" + amount + " Soul XP §ato §e" + target.getName().getString()), true);
                        return 1;
                    })
                )
            )
        );
    }
    
    /**
     * Calculate sell price for any item
     * Base prices + material bonuses
     */
    private static long calculateSellPrice(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.Item item = stack.getItem();
        String itemName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        
        // Special items with fixed prices
        if (item == net.minecraft.world.item.Items.DIAMOND) return 50;
        if (item == net.minecraft.world.item.Items.DIAMOND_BLOCK) return 450;
        if (item == net.minecraft.world.item.Items.GOLD_INGOT) return 15;
        if (item == net.minecraft.world.item.Items.GOLD_BLOCK) return 135;
        if (item == net.minecraft.world.item.Items.IRON_INGOT) return 10;
        if (item == net.minecraft.world.item.Items.IRON_BLOCK) return 90;
        if (item == net.minecraft.world.item.Items.COPPER_INGOT) return 5;
        if (item == net.minecraft.world.item.Items.COPPER_BLOCK) return 45;
        if (item == net.minecraft.world.item.Items.EMERALD) return 30;
        if (item == net.minecraft.world.item.Items.EMERALD_BLOCK) return 270;
        if (item == net.minecraft.world.item.Items.LAPIS_LAZULI) return 3;
        if (item == net.minecraft.world.item.Items.REDSTONE) return 2;
        if (item == net.minecraft.world.item.Items.COAL) return 2;
        if (item == net.minecraft.world.item.Items.NETHERITE_INGOT) return 500;
        if (item == net.minecraft.world.item.Items.NETHERITE_BLOCK) return 4500;
        if (item == net.minecraft.world.item.Items.ANCIENT_DEBRIS) return 200;
        
        // Logs and planks
        if (itemName.endsWith("_log") || itemName.endsWith("_wood")) return 1;
        if (itemName.endsWith("_planks")) return 1;
        
        // Ores
        if (itemName.contains("ore")) return 8;
        if (itemName.contains("deepslate") && itemName.contains("ore")) return 10;
        if (itemName.contains("raw_")) return 8;
        
        // Food
        if (item == net.minecraft.world.item.Items.BREAD) return 1;
        if (item == net.minecraft.world.item.Items.COOKED_BEEF) return 3;
        if (item == net.minecraft.world.item.Items.COOKED_PORKCHOP) return 3;
        if (item == net.minecraft.world.item.Items.COOKED_CHICKEN) return 2;
        if (item == net.minecraft.world.item.Items.COOKED_MUTTON) return 2;
        if (item == net.minecraft.world.item.Items.COOKED_COD) return 2;
        if (item == net.minecraft.world.item.Items.COOKED_SALMON) return 3;
        if (item == net.minecraft.world.item.Items.GOLDEN_APPLE) return 100;
        if (item == net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) return 2500;
        
        // Tools (based on material)
        if (itemName.contains("diamond_")) return 150;
        if (itemName.contains("iron_")) return 30;
        if (itemName.contains("gold_") || itemName.contains("golden_")) return 20;
        if (itemName.contains("stone_")) return 5;
        if (itemName.contains("wooden_")) return 2;
        if (itemName.contains("netherite_")) return 600;
        
        // Armor
        if (itemName.contains("leather_")) return 5;
        if (itemName.contains("chainmail_")) return 25;
        
        // Building blocks
        if (itemName.endsWith("_block")) return 1;
        if (item == net.minecraft.world.item.Items.COBBLESTONE) return 1;
        if (item == net.minecraft.world.item.Items.STONE) return 1;
        if (item == net.minecraft.world.item.Items.DIRT) return 1;
        if (item == net.minecraft.world.item.Items.SAND) return 1;
        if (item == net.minecraft.world.item.Items.GRAVEL) return 1;
        if (item == net.minecraft.world.item.Items.GLASS) return 2;
        if (itemName.contains("brick")) return 2;
        
        // Mob drops
        if (item == net.minecraft.world.item.Items.ROTTEN_FLESH) return 1;
        if (item == net.minecraft.world.item.Items.BONE) return 2;
        if (item == net.minecraft.world.item.Items.GUNPOWDER) return 5;
        if (item == net.minecraft.world.item.Items.ENDER_PEARL) return 25;
        if (item == net.minecraft.world.item.Items.BLAZE_ROD) return 20;
        if (item == net.minecraft.world.item.Items.GHAST_TEAR) return 50;
        if (item == net.minecraft.world.item.Items.SLIME_BALL) return 5;
        if (item == net.minecraft.world.item.Items.LEATHER) return 3;
        if (item == net.minecraft.world.item.Items.FEATHER) return 1;
        if (item == net.minecraft.world.item.Items.STRING) return 2;
        if (item == net.minecraft.world.item.Items.SPIDER_EYE) return 3;
        if (item == net.minecraft.world.item.Items.PHANTOM_MEMBRANE) return 15;
        if (item == net.minecraft.world.item.Items.WITHER_SKELETON_SKULL) return 200;
        if (item == net.minecraft.world.item.Items.NETHER_STAR) return 1000;
        
        // Default: 1 coin per item
        return 1;
    }
}
