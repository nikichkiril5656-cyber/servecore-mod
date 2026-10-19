package com.example.servercore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Один лот на аукционе. */
public class Listing {
    public final long id;
    public final UUID seller;
    public final String sellerName;
    public final ItemStack stack;
    public final long price;
    public final long createdAt;
    public final long expiresAt;

    public Listing(long id, UUID seller, String sellerName, ItemStack stack, long price, long createdAt, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.stack = stack;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putLong("id", id);
        t.putUUID("seller", seller);
        t.putString("sellerName", sellerName);
        t.put("item", stack.save(registries));
        t.putLong("price", price);
        t.putLong("createdAt", createdAt);
        t.putLong("expiresAt", expiresAt);
        return t;
    }

    /** null, если предмет не удалось прочитать (например, удалён мод). */
    public static Listing load(CompoundTag t, HolderLookup.Provider registries) {
        if (!t.contains("item", Tag.TAG_COMPOUND)) return null;
        ItemStack stack = ItemStack.parse(registries, t.getCompound("item")).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return null;
        return new Listing(t.getLong("id"), t.getUUID("seller"), t.getString("sellerName"), stack,
                t.getLong("price"), t.getLong("createdAt"), t.getLong("expiresAt"));
    }
}
