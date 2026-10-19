package com.example.servercore.data;

import com.example.servercore.ServerCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Хранилище аукциона: активные лоты и «почта» (предметы, ожидающие получения:
 * истёкшие и снятые лоты, предметы, не влезшие в инвентарь).
 */
public class AuctionStore extends SavedData {
    private static final String NAME = "servercore_auction";

    private final List<Listing> listings = new ArrayList<>();
    private final Map<UUID, List<ItemStack>> claims = new HashMap<>();
    private long nextId = 1;

    public static AuctionStore get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(AuctionStore::new, AuctionStore::load, null), NAME);
    }

    public long nextId() {
        setDirty();
        return nextId++;
    }

    public List<Listing> all() {
        return listings;
    }

    public void add(Listing l) {
        listings.add(l);
        setDirty();
    }

    public void remove(Listing l) {
        listings.remove(l);
        setDirty();
    }

    public Listing find(long id) {
        for (Listing l : listings) {
            if (l.id == id) return l;
        }
        return null;
    }

    public int countBy(UUID seller) {
        int n = 0;
        for (Listing l : listings) {
            if (l.seller.equals(seller)) n++;
        }
        return n;
    }

    // ---- почта ----

    public void addClaim(UUID owner, ItemStack stack) {
        if (stack.isEmpty()) return;
        claims.computeIfAbsent(owner, k -> new ArrayList<>()).add(stack);
        setDirty();
    }

    /** Живой список ожидающих предметов игрока (изменения нужно завершать вызовом touch()). */
    public List<ItemStack> claimsOf(UUID owner) {
        return claims.computeIfAbsent(owner, k -> new ArrayList<>());
    }

    public int claimCount(UUID owner) {
        List<ItemStack> l = claims.get(owner);
        return l == null ? 0 : l.size();
    }

    public void touch() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("nextId", nextId);
        ListTag ls = new ListTag();
        for (Listing l : listings) ls.add(l.save(registries));
        tag.put("listings", ls);

        CompoundTag cl = new CompoundTag();
        for (Map.Entry<UUID, List<ItemStack>> e : claims.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            ListTag items = new ListTag();
            for (ItemStack s : e.getValue()) items.add(s.save(registries));
            cl.put(e.getKey().toString(), items);
        }
        tag.put("claims", cl);
        return tag;
    }

    public static AuctionStore load(CompoundTag tag, HolderLookup.Provider registries) {
        AuctionStore s = new AuctionStore();
        s.nextId = Math.max(1, tag.getLong("nextId"));
        ListTag ls = tag.getList("listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < ls.size(); i++) {
            Listing l = Listing.load(ls.getCompound(i), registries);
            if (l != null) s.listings.add(l);
            else ServerCore.LOGGER.warn("Не удалось прочитать лот аукциона #{}, пропущен", i);
        }
        CompoundTag cl = tag.getCompound("claims");
        for (String key : cl.getAllKeys()) {
            UUID owner;
            try {
                owner = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ListTag items = cl.getList(key, Tag.TAG_COMPOUND);
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                ItemStack st = ItemStack.parse(registries, items.getCompound(i)).orElse(ItemStack.EMPTY);
                if (!st.isEmpty()) list.add(st);
            }
            if (!list.isEmpty()) s.claims.put(owner, list);
        }
        return s;
    }
}
