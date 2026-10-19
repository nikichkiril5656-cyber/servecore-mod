package com.example.servercore.skill;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

public enum Skill {
    COMBAT("Бой", Items.IRON_SWORD, ChatFormatting.RED,
            "+0.5% урона за уровень"),
    MINING("Добыча", Items.IRON_PICKAXE, ChatFormatting.AQUA,
            "+1% скорости добычи и +0.5% шанс двойного дропа с руд за уровень"),
    FARMING("Фермерство", Items.IRON_HOE, ChatFormatting.GREEN,
            "+0.8% шанс двойного урожая за уровень"),
    FORAGING("Лесорубство", Items.IRON_AXE, ChatFormatting.DARK_GREEN,
            "+1% скорости рубки и +0.8% шанс двойного дропа с брёвен за уровень"),
    FISHING("Рыбалка", Items.FISHING_ROD, ChatFormatting.BLUE,
            "+0.8% шанс двойного улова и бонус монет за каждый улов");

    public final String title;
    public final Item icon;
    public final ChatFormatting color;
    public final String bonusText;

    Skill(String title, Item icon, ChatFormatting color, String bonusText) {
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.bonusText = bonusText;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Skill byId(String id) {
        for (Skill s : values()) {
            if (s.id().equalsIgnoreCase(id)) return s;
        }
        return null;
    }
}
