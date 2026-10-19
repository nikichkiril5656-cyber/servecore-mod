package com.example.servercore.skill;

import com.example.servercore.Config;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.pet.PetManager;
import com.example.servercore.util.Fmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Опыт, уровни и бонусы навыков. */
public final class SkillManager {
    // Бонусы за уровень навыка
    public static final double COMBAT_DAMAGE = 0.005;
    public static final double MINING_SPEED = 0.01;
    public static final double MINING_DOUBLE = 0.005;
    public static final double FARMING_DOUBLE = 0.008;
    public static final double FORAGING_SPEED = 0.01;
    public static final double FORAGING_DOUBLE = 0.008;
    public static final double FISHING_DOUBLE = 0.008;
    public static final long FISHING_COINS_PER_LEVEL = 2;

    private SkillManager() {
    }

    /** Опыт, нужный для перехода с уровня level на level + 1. */
    public static long xpForNext(int level) {
        return (long) (40 + 6 * Math.pow(level, 1.6));
    }

    /** Суммарный опыт, нужный чтобы достичь уровня level. */
    public static long totalXpForLevel(int level) {
        long sum = 0;
        for (int i = 0; i < level; i++) sum += xpForNext(i);
        return sum;
    }

    public static int levelOf(long totalXp) {
        int max = Config.SKILL_MAX_LEVEL.get();
        int lvl = 0;
        long rem = totalXp;
        while (lvl < max) {
            long need = xpForNext(lvl);
            if (rem < need) break;
            rem -= need;
            lvl++;
        }
        return lvl;
    }

    /** Опыт внутри текущего уровня. */
    public static long xpIntoLevel(long totalXp) {
        int lvl = levelOf(totalXp);
        return totalXp - totalXpForLevel(lvl);
    }

    public static int level(ServerPlayer p, Skill s) {
        return levelOf(PlayerStore.get(p.server).data(p).xp(s));
    }

    /** Выдать опыт навыка с учётом множителей и бонусов питомца. */
    public static void addXp(ServerPlayer player, Skill skill, double baseXp) {
        if (baseXp <= 0) return;
        PlayerStore store = PlayerStore.get(player.server);
        PlayerData d = store.data(player);

        double mult = Config.SKILL_XP_MULT.get() * (1.0 + PetManager.xpBonus(d, skill));
        long amount = Math.max(1, Math.round(baseXp * mult));

        long before = d.xp(skill);
        int oldLevel = levelOf(before);
        long after = before + amount;
        d.skillXp.put(skill, after);
        int newLevel = levelOf(after);
        store.setDirty();

        int max = Config.SKILL_MAX_LEVEL.get();
        if (newLevel >= max) {
            player.displayClientMessage(Component.literal("+" + Fmt.num(amount) + " " + skill.title + " (МАКС.)")
                    .withStyle(skill.color), true);
        } else {
            player.displayClientMessage(Component.literal("+" + Fmt.num(amount) + " " + skill.title + " ("
                    + Fmt.num(xpIntoLevel(after)) + "/" + Fmt.num(xpForNext(newLevel)) + ")")
                    .withStyle(skill.color), true);
        }

        for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
            long reward = Config.SKILL_LEVEL_REWARD.get() * lvl;
            d.balance = Fmt.satAdd(d.balance, reward);
            player.sendSystemMessage(Component.literal("Навык ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(skill.title).withStyle(skill.color))
                    .append(Component.literal(" достиг уровня " + lvl + "! Награда: " + Fmt.coins(reward))
                            .withStyle(ChatFormatting.GOLD)));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Питомец получает опыт вместе с навыком
        PetManager.addXp(player, d, Math.round(amount * Config.PET_XP_SHARE.get()));
    }

    /** Установить уровень навыка напрямую (админ-команда). */
    public static void setLevel(ServerPlayer player, Skill skill, int level) {
        PlayerStore store = PlayerStore.get(player.server);
        int clamped = Math.max(0, Math.min(Config.SKILL_MAX_LEVEL.get(), level));
        store.data(player).skillXp.put(skill, totalXpForLevel(clamped));
        store.setDirty();
    }
}
