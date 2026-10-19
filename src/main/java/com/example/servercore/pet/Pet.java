package com.example.servercore.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

import java.util.UUID;

public class Pet {
    public static final int MAX_LEVEL = 100;

    public final UUID id;
    public final PetType type;
    public final PetRarity rarity;
    public int level;
    /** Опыт внутри текущего уровня. */
    public long xp;

    public Pet(UUID id, PetType type, PetRarity rarity, int level, long xp) {
        this.id = id;
        this.type = type;
        this.rarity = rarity;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.xp = xp;
    }

    public static Pet create(PetType type, PetRarity rarity, int level) {
        return new Pet(UUID.randomUUID(), type, rarity, level, 0);
    }

    public static Pet random(RandomSource rnd) {
        return create(PetType.random(rnd), PetRarity.random(rnd), 1);
    }

    /** Опыт, нужный чтобы перейти с уровня level на следующий. */
    public static long xpForNext(int level) {
        return (long) (25 + 0.5 * level * level);
    }

    public double progress() {
        if (level >= MAX_LEVEL) return 1.0;
        return xp / (double) xpForNext(level);
    }

    /** Добавляет опыт, возвращает количество полученных уровней. */
    public int addXp(long amount) {
        if (level >= MAX_LEVEL || amount <= 0) return 0;
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL) {
            long need = xpForNext(level);
            if (xp < need) break;
            xp -= need;
            level++;
            gained++;
        }
        if (level >= MAX_LEVEL) xp = 0;
        return gained;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("type", type.id());
        t.putString("rarity", rarity.id());
        t.putInt("level", level);
        t.putLong("xp", xp);
        return t;
    }

    /** null, если тип или редкость больше не существуют. */
    public static Pet load(CompoundTag t) {
        if (!t.hasUUID("id")) return null;
        PetType type = PetType.byId(t.getString("type"));
        PetRarity rarity = PetRarity.byId(t.getString("rarity"));
        if (type == null || rarity == null) return null;
        return new Pet(t.getUUID("id"), type, rarity, Math.max(1, t.getInt("level")), t.getLong("xp"));
    }
}
