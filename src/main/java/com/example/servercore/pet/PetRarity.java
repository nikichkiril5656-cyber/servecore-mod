package com.example.servercore.pet;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public enum PetRarity {
    COMMON("Обычный", ChatFormatting.GRAY, 1.00, 60),
    UNCOMMON("Необычный", ChatFormatting.GREEN, 1.15, 25),
    RARE("Редкий", ChatFormatting.BLUE, 1.35, 10),
    EPIC("Эпический", ChatFormatting.DARK_PURPLE, 1.60, 4),
    LEGENDARY("Легендарный", ChatFormatting.GOLD, 2.00, 1);

    public final String title;
    public final ChatFormatting color;
    /** Множитель всех бонусов питомца. */
    public final double multiplier;
    /** Вес при случайной генерации. */
    public final double weight;

    PetRarity(String title, ChatFormatting color, double multiplier, double weight) {
        this.title = title;
        this.color = color;
        this.multiplier = multiplier;
        this.weight = weight;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PetRarity byId(String id) {
        for (PetRarity r : values()) {
            if (r.id().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    public static PetRarity random(net.minecraft.util.RandomSource rnd) {
        double total = 0;
        for (PetRarity r : values()) total += r.weight;
        double roll = rnd.nextDouble() * total;
        for (PetRarity r : values()) {
            roll -= r.weight;
            if (roll < 0) return r;
        }
        return COMMON;
    }
}
