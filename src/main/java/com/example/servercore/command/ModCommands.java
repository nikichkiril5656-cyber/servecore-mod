package com.example.servercore.command;

import com.example.servercore.Config;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.menu.AuctionGui;
import com.example.servercore.menu.PetsGui;
import com.example.servercore.menu.SkillsGui;
import com.example.servercore.pet.Pet;
import com.example.servercore.pet.PetManager;
import com.example.servercore.pet.PetRarity;
import com.example.servercore.pet.PetType;
import com.example.servercore.auction.AuctionManager;
import com.example.servercore.skill.Skill;
import com.example.servercore.skill.SkillManager;
import com.example.servercore.util.Fmt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/** Регистрация всех команд: /ah, /balance, /pay, /eco, /skills, /pets. */
public final class ModCommands {
    private ModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        registerAuction(d);
        registerEconomy(d);
        registerSkills(d);
        registerPets(d);
    }

    private static boolean isOp(CommandSourceStack src) {
        return src.hasPermission(2);
    }

    // ---------- /ah ----------

    private static void registerAuction(CommandDispatcher<CommandSourceStack> d) {
        LiteralCommandNode<CommandSourceStack> ah = d.register(Commands.literal("ah")
                .executes(c -> {
                    AuctionGui.open(c.getSource().getPlayerOrException(), false);
                    return 1;
                })
                .then(Commands.literal("my").executes(c -> {
                    AuctionGui.open(c.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("claim").executes(c -> {
                    AuctionManager.claim(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("sell")
                        .then(Commands.argument("price", LongArgumentType.longArg(1))
                                .executes(c -> {
                                    AuctionManager.sell(c.getSource().getPlayerOrException(),
                                            LongArgumentType.getLong(c, "price"), 0);
                                    return 1;
                                })
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 99))
                                        .executes(c -> {
                                            AuctionManager.sell(c.getSource().getPlayerOrException(),
                                                    LongArgumentType.getLong(c, "price"),
                                                    IntegerArgumentType.getInteger(c, "amount"));
                                            return 1;
                                        })))));
        d.register(Commands.literal("auction").redirect(ah));
    }

    // ---------- экономика ----------

    private static void registerEconomy(CommandDispatcher<CommandSourceStack> d) {
        LiteralCommandNode<CommandSourceStack> bal = d.register(Commands.literal("balance")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Fmt.ok(p, "Ваш баланс: " + Fmt.coins(PlayerStore.get(p.server).data(p).balance));
                    return 1;
                }));
        d.register(Commands.literal("bal").redirect(bal));
        d.register(Commands.literal("money").redirect(bal));

        d.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(c -> {
                                    ServerPlayer from = c.getSource().getPlayerOrException();
                                    ServerPlayer to = EntityArgument.getPlayer(c, "player");
                                    long amount = LongArgumentType.getLong(c, "amount");
                                    if (from.getUUID().equals(to.getUUID())) {
                                        Fmt.err(from, "Нельзя перевести деньги самому себе.");
                                        return 0;
                                    }
                                    PlayerStore store = PlayerStore.get(from.server);
                                    PlayerData fd = store.data(from);
                                    if (fd.balance < amount) {
                                        Fmt.err(from, "Недостаточно средств. Ваш баланс: " + Fmt.coins(fd.balance));
                                        return 0;
                                    }
                                    PlayerData td = store.data(to);
                                    fd.balance -= amount;
                                    td.balance = Fmt.satAdd(td.balance, amount);
                                    store.setDirty();
                                    Fmt.ok(from, "Вы перевели " + Fmt.coins(amount) + " игроку " + to.getGameProfile().getName() + ".");
                                    Fmt.ok(to, from.getGameProfile().getName() + " перевёл вам " + Fmt.coins(amount) + ".");
                                    return 1;
                                }))));

        d.register(Commands.literal("eco")
                .requires(ModCommands::isOp)
                .then(Commands.literal("give").then(ecoArgs((p, data, amount) -> data.balance = Fmt.satAdd(data.balance, amount))))
                .then(Commands.literal("take").then(ecoArgs((p, data, amount) -> data.balance = Math.max(0, data.balance - amount))))
                .then(Commands.literal("set").then(ecoArgs((p, data, amount) -> data.balance = amount))));
    }

    @FunctionalInterface
    private interface EcoAction {
        void apply(ServerPlayer player, PlayerData data, long amount);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> ecoArgs(EcoAction action) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                        .executes(c -> {
                            ServerPlayer target = EntityArgument.getPlayer(c, "player");
                            long amount = LongArgumentType.getLong(c, "amount");
                            PlayerStore store = PlayerStore.get(target.server);
                            PlayerData data = store.data(target);
                            action.apply(target, data, amount);
                            store.setDirty();
                            long bal = data.balance;
                            c.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().getName()
                                    + ": баланс " + Fmt.coins(bal)), true);
                            return 1;
                        }));
    }

    // ---------- /skills ----------

    private static void registerSkills(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("skills")
                .executes(c -> {
                    SkillsGui.open(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("setlevel")
                        .requires(ModCommands::isOp)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Skill.values()).map(Skill::id), b))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .executes(c -> {
                                                    ServerPlayer t = EntityArgument.getPlayer(c, "player");
                                                    Skill s = skill(c);
                                                    if (s == null) return 0;
                                                    int level = IntegerArgumentType.getInteger(c, "level");
                                                    SkillManager.setLevel(t, s, level);
                                                    c.getSource().sendSuccess(() -> Component.literal(
                                                            t.getGameProfile().getName() + ": " + s.title
                                                                    + " -> уровень " + SkillManager.level(t, s)), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("addxp")
                        .requires(ModCommands::isOp)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Skill.values()).map(Skill::id), b))
                                        .then(Commands.argument("xp", IntegerArgumentType.integer(1))
                                                .executes(c -> {
                                                    ServerPlayer t = EntityArgument.getPlayer(c, "player");
                                                    Skill s = skill(c);
                                                    if (s == null) return 0;
                                                    SkillManager.addXp(t, s, IntegerArgumentType.getInteger(c, "xp"));
                                                    return 1;
                                                }))))));
    }

    private static Skill skill(CommandContext<CommandSourceStack> c) {
        Skill s = Skill.byId(StringArgumentType.getString(c, "skill"));
        if (s == null) {
            c.getSource().sendFailure(Component.literal("Неизвестный навык. Доступны: "
                    + String.join(", ", Arrays.stream(Skill.values()).map(Skill::id).toList())));
        }
        return s;
    }

    // ---------- /pets ----------

    private static void registerPets(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("pets")
                .executes(c -> {
                    PetsGui.open(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("buy").executes(c -> {
                    return PetManager.buyEgg(c.getSource().getPlayerOrException()) ? 1 : 0;
                }))
                .then(Commands.literal("give")
                        .requires(ModCommands::isOp)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(PetType.values()).map(PetType::id), b))
                                        .then(Commands.argument("rarity", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(PetRarity.values()).map(PetRarity::id), b))
                                                .executes(c -> give(c, 1))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, Pet.MAX_LEVEL))
                                                        .executes(c -> give(c, IntegerArgumentType.getInteger(c, "level")))))))));
    }

    private static int give(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "player");
        PetType type = PetType.byId(StringArgumentType.getString(c, "type"));
        PetRarity rarity = PetRarity.byId(StringArgumentType.getString(c, "rarity"));
        if (type == null) {
            c.getSource().sendFailure(Component.literal("Неизвестный тип питомца. Доступны: "
                    + String.join(", ", Arrays.stream(PetType.values()).map(PetType::id).toList())));
            return 0;
        }
        if (rarity == null) {
            c.getSource().sendFailure(Component.literal("Неизвестная редкость. Доступны: "
                    + String.join(", ", Arrays.stream(PetRarity.values()).map(PetRarity::id).toList())));
            return 0;
        }
        PetManager.grant(target, Pet.create(type, rarity, level), "выдан администратором");
        c.getSource().sendSuccess(() -> Component.literal("Питомец выдан игроку " + target.getGameProfile().getName()), true);
        return 1;
    }
}
