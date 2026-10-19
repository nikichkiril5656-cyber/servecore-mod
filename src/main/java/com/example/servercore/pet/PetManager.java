package com.example.servercore.pet;

import com.example.servercore.Config;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.skill.Skill;
import com.example.servercore.util.Fmt;
import com.example.servercore.util.Inv;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Выдача, экипировка, опыт и предметная форма питомцев. */
public final class PetManager {
    private static final String ITEM_KEY = "servercore_pet";

    private PetManager() {
    }

    // ---------- бонусы ----------

    /** Суммарный бонус активного питомца к опыту навыка (0.3 = +30%). */
    public static double xpBonus(PlayerData d, Skill skill) {
        Pet pet = d.activePet();
        if (pet == null) return 0;
        double sum = 0;
        for (PetStat s : pet.type.stats) {
            if (s.kind().skill == skill) sum += s.value(pet);
        }
        return sum;
    }

    /** Пересчитывает атрибуты игрока по активному питомцу. Вызывать при входе, респавне, смене питомца и повышении уровня. */
    public static void applyAttributes(ServerPlayer player) {
        PlayerData d = PlayerStore.get(player.server).data(player);
        Pet pet = d.activePet();
        for (StatKind kind : StatKind.values()) {
            if (kind.attribute == null) continue;
            AttributeInstance inst = player.getAttribute(kind.attribute);
            if (inst == null) continue;
            inst.removeModifier(kind.modifierId());
        }
        if (pet == null) return;
        for (PetStat s : pet.type.stats) {
            StatKind kind = s.kind();
            if (kind.attribute == null) continue;
            AttributeInstance inst = player.getAttribute(kind.attribute);
            if (inst == null) continue;
            inst.addOrUpdateTransientModifier(
                    new AttributeModifier(kind.modifierId(), s.value(pet), kind.operation));
        }
    }

    // ---------- опыт ----------

    public static void addXp(ServerPlayer player, PlayerData d, long amount) {
        Pet pet = d.activePet();
        if (pet == null || amount <= 0 || pet.level >= Pet.MAX_LEVEL) return;
        int gained = pet.addXp(amount);
        PlayerStore.get(player.server).setDirty();
        if (gained > 0) {
            player.sendSystemMessage(Component.literal("Ваш питомец ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(pet.type.title).withStyle(pet.rarity.color))
                    .append(Component.literal(" достиг уровня " + pet.level + "!").withStyle(ChatFormatting.AQUA)));
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            applyAttributes(player);
        }
    }

    // ---------- владение ----------

    public static boolean isFull(PlayerData d) {
        return d.pets.size() >= Config.PET_MAX_OWNED.get();
    }

    /**
     * Выдаёт питомца: в список, а если список полон — предметом в инвентарь,
     * так что питомец никогда не теряется.
     */
    public static void grant(ServerPlayer player, Pet pet, String source) {
        PlayerData d = PlayerStore.get(player.server).data(player);
        player.sendSystemMessage(Component.literal("Вы получили питомца: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(pet.rarity.title + " " + pet.type.title).withStyle(pet.rarity.color))
                .append(Component.literal(" (" + source + ")").withStyle(ChatFormatting.GRAY)));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.5f);
        if (!isFull(d)) {
            d.pets.add(pet);
            PlayerStore.get(player.server).setDirty();
        } else {
            Fmt.info(player, "Список питомцев полон, питомец выдан предметом.");
            Inv.give(player, toItem(pet));
        }
    }

    /** Шанс получить питомца за действие. */
    public static void tryDrop(ServerPlayer player, double chance, String source) {
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) return;
        grant(player, Pet.random(player.getRandom()), source);
    }

    public static boolean buyEgg(ServerPlayer player) {
        PlayerStore store = PlayerStore.get(player.server);
        PlayerData d = store.data(player);
        long price = Config.PET_EGG_PRICE.get();
        if (isFull(d)) {
            Fmt.err(player, "Список питомцев полон (" + Config.PET_MAX_OWNED.get() + "). Освободите место.");
            return false;
        }
        if (d.balance < price) {
            Fmt.err(player, "Недостаточно средств. Нужно: " + Fmt.coins(price));
            return false;
        }
        d.balance -= price;
        store.setDirty();
        grant(player, Pet.random(player.getRandom()), "яйцо за " + Fmt.coins(price));
        return true;
    }

    /** Экипировать питомца; petId == null — снять текущего. */
    public static void equip(ServerPlayer player, UUID petId) {
        PlayerStore store = PlayerStore.get(player.server);
        PlayerData d = store.data(player);
        if (petId != null && d.pet(petId) == null) return;
        d.activePet = petId;
        store.setDirty();
        applyAttributes(player);
    }

    /** Превращает питомца в предмет (его можно продать на аукционе или передать). */
    public static boolean extract(ServerPlayer player, UUID petId) {
        PlayerStore store = PlayerStore.get(player.server);
        PlayerData d = store.data(player);
        Pet pet = d.pet(petId);
        if (pet == null) return false;
        if (petId.equals(d.activePet)) {
            d.activePet = null;
        }
        d.pets.remove(pet);
        store.setDirty();
        applyAttributes(player);
        Inv.give(player, toItem(pet));
        return true;
    }

    /** Получение питомца из предмета (ПКМ). */
    public static void redeem(ServerPlayer player, ItemStack stack, Pet pet) {
        PlayerStore store = PlayerStore.get(player.server);
        PlayerData d = store.data(player);
        if (d.pet(pet.id) != null) {
            Fmt.err(player, "Такой питомец у вас уже есть.");
            return;
        }
        if (isFull(d)) {
            Fmt.err(player, "Список питомцев полон (" + Config.PET_MAX_OWNED.get() + ").");
            return;
        }
        stack.shrink(1);
        d.pets.add(pet);
        store.setDirty();
        Fmt.ok(player, "Питомец добавлен в коллекцию: /pets");
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    // ---------- предметная форма ----------

    public static ItemStack toItem(Pet pet) {
        ItemStack s = new ItemStack(Items.HEART_OF_THE_SEA);
        s.set(DataComponents.CUSTOM_NAME, Fmt.text("Питомец: " + pet.type.title + " [ур. " + pet.level + "]", pet.rarity.color));
        List<Component> lore = lore(pet, false);
        lore.add(Component.empty());
        lore.add(Fmt.text("ПКМ — добавить в коллекцию питомцев", ChatFormatting.YELLOW));
        s.set(DataComponents.LORE, new ItemLore(lore));
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        s.set(DataComponents.MAX_STACK_SIZE, 1);
        CompoundTag root = new CompoundTag();
        root.put(ITEM_KEY, pet.save());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return s;
    }

    /** null, если предмет — не питомец. */
    public static Pet fromItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag tag = cd.copyTag();
        if (!tag.contains(ITEM_KEY, Tag.TAG_COMPOUND)) return null;
        return Pet.load(tag.getCompound(ITEM_KEY));
    }

    // ---------- описание ----------

    public static List<Component> lore(Pet pet, boolean active) {
        List<Component> l = new ArrayList<>();
        l.add(Fmt.text(pet.rarity.title + " питомец", pet.rarity.color));
        l.add(Fmt.text("Уровень: " + pet.level + " / " + Pet.MAX_LEVEL, ChatFormatting.GRAY));
        if (pet.level < Pet.MAX_LEVEL) {
            l.add(Fmt.bar(pet.progress(), 24));
            l.add(Fmt.text(Fmt.num(pet.xp) + " / " + Fmt.num(Pet.xpForNext(pet.level)) + " опыта", ChatFormatting.DARK_GRAY));
        } else {
            l.add(Fmt.text("МАКСИМАЛЬНЫЙ УРОВЕНЬ", ChatFormatting.GOLD));
        }
        l.add(Component.empty());
        l.add(Fmt.text("Бонусы:", ChatFormatting.GRAY));
        for (PetStat s : pet.type.stats) {
            l.add(Fmt.text(" " + s.kind().title + " " + s.kind().format(s.value(pet)), ChatFormatting.GREEN));
        }
        if (active) {
            l.add(Component.empty());
            l.add(Fmt.text("Активен", ChatFormatting.AQUA));
        }
        return l;
    }
}
