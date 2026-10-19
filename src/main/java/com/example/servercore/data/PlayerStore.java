package com.example.servercore.data;

import com.example.servercore.Config;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Постоянное хранилище данных игроков (world/data/servercore_players.dat). */
public class PlayerStore extends SavedData {
    private static final String NAME = "servercore_players";

    private final Map<UUID, PlayerData> players = new HashMap<>();

    public static PlayerStore get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(PlayerStore::new, PlayerStore::load, null), NAME);
    }

    public PlayerData data(ServerPlayer player) {
        PlayerData d = data(player.getUUID());
        String n = player.getGameProfile().getName();
        if (!n.equals(d.name)) {
            d.name = n;
            setDirty();
        }
        return d;
    }

    public PlayerData data(UUID id) {
        return players.computeIfAbsent(id, k -> {
            PlayerData d = new PlayerData(k);
            d.balance = Config.START_BALANCE.get();
            setDirty();
            return d;
        });
    }

    public boolean has(UUID id) {
        return players.containsKey(id);
    }

    /** Найти игрока по последнему известному нику. */
    public PlayerData byName(String name) {
        for (PlayerData d : players.values()) {
            if (d.name.equalsIgnoreCase(name)) return d;
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (PlayerData d : players.values()) list.add(d.save());
        tag.put("players", list);
        return tag;
    }

    public static PlayerStore load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerStore s = new PlayerStore();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PlayerData d = PlayerData.load(list.getCompound(i));
            s.players.put(d.id, d);
        }
        return s;
    }
}
