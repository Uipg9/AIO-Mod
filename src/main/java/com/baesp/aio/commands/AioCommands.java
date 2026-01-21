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
        
        // /skills - Open skills GUI
        dispatcher.register(Commands.literal("skills")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                SkillsScreen.open(player);
                return 1;
            })
        );
        
        // /ascend - Open ascendancy GUI
        dispatcher.register(Commands.literal("ascend")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                AscendancyScreen.open(player);
                return 1;
            })
        );
        
        // /shop - Open shop GUI
        dispatcher.register(Commands.literal("shop")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ShopScreen.open(player);
                return 1;
            })
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
}
