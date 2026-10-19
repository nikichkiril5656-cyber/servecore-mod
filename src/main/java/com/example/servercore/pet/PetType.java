package com.example.servercore.pet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

public enum PetType {
    WOLF("Волк", Items.WOLF_SPAWN_EGG, 18,
            new PetStat(StatKind.ATTACK_DAMAGE, 4.0), new PetStat(StatKind.XP_COMBAT, 0.25)),
    SILVERFISH("Чешуйница", Items.SILVERFISH_SPAWN_EGG, 14,
            new PetStat(StatKind.BREAK_SPEED, 0.20), new PetStat(StatKind.XP_MINING, 0.30)),
    BEE("Пчела", Items.BEE_SPAWN_EGG, 14,
            new PetStat(StatKind.XP_FARMING, 0.30), new PetStat(StatKind.MOVEMENT_SPEED, 0.06)),
    DOLPHIN("Дельфин", Items.DOLPHIN_SPAWN_EGG, 14,
            new PetStat(StatKind.XP_FISHING, 0.30), new PetStat(StatKind.LUCK, 2.0)),
    PANDA("Панда", Items.PANDA_SPAWN_EGG, 12,
            new PetStat(StatKind.XP_FORAGING, 0.30), new PetStat(StatKind.MAX_HEALTH, 4.0)),
    ARMADILLO("Броненосец", Items.ARMADILLO_SPAWN_EGG, 12,
            new PetStat(StatKind.ARMOR, 6.0), new PetStat(StatKind.MAX_HEALTH, 6.0)),
    HORSE("Лошадь", Items.HORSE_SPAWN_EGG, 10,
            new PetStat(StatKind.MOVEMENT_SPEED, 0.15), new PetStat(StatKind.MAX_HEALTH, 4.0)),
    ENDERMAN("Странник Края", Items.ENDERMAN_SPAWN_EGG, 6,
            new PetStat(StatKind.LUCK, 3.0), new PetStat(StatKind.XP_MINING, 0.15)),
    WARDEN("Варден", Items.WARDEN_SPAWN_EGG, 2,
            new PetStat(StatKind.ATTACK_DAMAGE, 6.0), new PetStat(StatKind.MAX_HEALTH, 10.0));

    public final String title;
    public final Item icon;
    public final double weight;
    public final List<PetStat> stats;

    PetType(String title, Item icon, double weight, PetStat... stats) {
        this.title = title;
        this.icon = icon;
        this.weight = weight;
        this.stats = List.of(stats);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PetType byId(String id) {
        for (PetType t : values()) {
            if (t.id().equalsIgnoreCase(id)) return t;
        }
        return null;
    }

    public static PetType random(RandomSource rnd) {
        double total = 0;
        for (PetType t : values()) total += t.weight;
        double roll = rnd.nextDouble() * total;
        for (PetType t : values()) {
            roll -= t.weight;
            if (roll < 0) return t;
        }
        return WOLF;
    }
}
