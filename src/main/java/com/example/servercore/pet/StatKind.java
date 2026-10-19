package com.example.servercore.pet;

import com.example.servercore.ServerCore;
import com.example.servercore.skill.Skill;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Locale;

/** Тип бонуса питомца: либо атрибут игрока, либо бонус к опыту навыка. */
public enum StatKind {
    ATTACK_DAMAGE("Урон", Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE, false, null),
    MAX_HEALTH("Здоровье", Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE, false, null),
    ARMOR("Броня", Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, false, null),
    LUCK("Удача", Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE, false, null),
    MOVEMENT_SPEED("Скорость", Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true, null),
    BREAK_SPEED("Скорость добычи", Attributes.BLOCK_BREAK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true, null),
    XP_COMBAT("Опыт: Бой", null, null, true, Skill.COMBAT),
    XP_MINING("Опыт: Добыча", null, null, true, Skill.MINING),
    XP_FARMING("Опыт: Фермерство", null, null, true, Skill.FARMING),
    XP_FORAGING("Опыт: Лесорубство", null, null, true, Skill.FORAGING),
    XP_FISHING("Опыт: Рыбалка", null, null, true, Skill.FISHING);

    public final String title;
    /** null для бонусов к опыту. */
    public final Holder<Attribute> attribute;
    public final AttributeModifier.Operation operation;
    public final boolean percent;
    /** Навык, к опыту которого применяется бонус (для XP_*). */
    public final Skill skill;

    StatKind(String title, Holder<Attribute> attribute, AttributeModifier.Operation operation,
             boolean percent, Skill skill) {
        this.title = title;
        this.attribute = attribute;
        this.operation = operation;
        this.percent = percent;
        this.skill = skill;
    }

    public ResourceLocation modifierId() {
        return ResourceLocation.fromNamespaceAndPath(ServerCore.MODID, "pet_" + name().toLowerCase(Locale.ROOT));
    }

    public String format(double value) {
        if (percent) return String.format(Locale.US, "+%.1f%%", value * 100.0);
        return String.format(Locale.US, "+%.1f", value);
    }
}
