package com.example.servercore.data;

import com.example.servercore.pet.Pet;
import com.example.servercore.skill.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

/** Все данные одного игрока: баланс, опыт навыков, питомцы. */
public class PlayerData {
    public final UUID id;
    public String name = "?";
    public long balance;
    public final EnumMap<Skill, Long> skillXp = new EnumMap<>(Skill.class);
    public final List<Pet> pets = new ArrayList<>();
    public UUID activePet;
    /** Заработок с продаж на аукционе, пока игрок был оффлайн. */
    public long offlineEarnings;
    public int offlineSales;

    public PlayerData(UUID id) {
        this.id = id;
    }

    public long xp(Skill s) {
        return skillXp.getOrDefault(s, 0L);
    }

    public Pet pet(UUID petId) {
        if (petId == null) return null;
        for (Pet p : pets) {
            if (p.id.equals(petId)) return p;
        }
        return null;
    }

    public Pet activePet() {
        return pet(activePet);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("name", name);
        t.putLong("balance", balance);
        CompoundTag skills = new CompoundTag();
        for (Skill s : Skill.values()) {
            skills.putLong(s.id(), xp(s));
        }
        t.put("skills", skills);
        ListTag list = new ListTag();
        for (Pet p : pets) list.add(p.save());
        t.put("pets", list);
        if (activePet != null) t.putUUID("activePet", activePet);
        t.putLong("offlineEarnings", offlineEarnings);
        t.putInt("offlineSales", offlineSales);
        return t;
    }

    public static PlayerData load(CompoundTag t) {
        PlayerData d = new PlayerData(t.getUUID("id"));
        d.name = t.getString("name");
        d.balance = t.getLong("balance");
        CompoundTag skills = t.getCompound("skills");
        for (Skill s : Skill.values()) {
            if (skills.contains(s.id())) d.skillXp.put(s, skills.getLong(s.id()));
        }
        ListTag list = t.getList("pets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Pet p = Pet.load(list.getCompound(i));
            if (p != null) d.pets.add(p);
        }
        if (t.hasUUID("activePet")) {
            UUID a = t.getUUID("activePet");
            d.activePet = d.pet(a) != null ? a : null;
        }
        d.offlineEarnings = t.getLong("offlineEarnings");
        d.offlineSales = t.getInt("offlineSales");
        return d;
    }
}
