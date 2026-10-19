package com.example.servercore.auction;

import com.example.servercore.Config;
import com.example.servercore.data.AuctionStore;
import com.example.servercore.data.Listing;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.util.Fmt;
import com.example.servercore.util.Inv;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Бизнес-логика аукциона: выставление, покупка, снятие, истечение, почта. */
public final class AuctionManager {
    private AuctionManager() {
    }

    // ---------- фильтры и сортировка ----------

    public enum Category {
        ALL("Все"),
        WEAPONS("Оружие"),
        TOOLS("Инструменты"),
        ARMOR("Броня"),
        BLOCKS("Блоки"),
        FOOD("Еда"),
        MISC("Прочее");

        public final String title;

        Category(String title) {
            this.title = title;
        }

        public Category next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean matches(ItemStack s) {
            return switch (this) {
                case ALL -> true;
                case WEAPONS -> isWeapon(s);
                case TOOLS -> isTool(s);
                case ARMOR -> isArmor(s);
                case BLOCKS -> isBlock(s);
                case FOOD -> isFood(s);
                case MISC -> !isWeapon(s) && !isTool(s) && !isArmor(s) && !isBlock(s) && !isFood(s);
            };
        }

        private static boolean isWeapon(ItemStack s) {
            return s.is(ItemTags.SWORDS) || s.getItem() instanceof ProjectileWeaponItem
                    || s.getItem() instanceof TridentItem || s.getItem() instanceof MaceItem;
        }

        private static boolean isTool(ItemStack s) {
            return s.is(ItemTags.PICKAXES) || s.is(ItemTags.AXES) || s.is(ItemTags.SHOVELS) || s.is(ItemTags.HOES)
                    || s.getItem() instanceof ShearsItem || s.getItem() instanceof FishingRodItem;
        }

        private static boolean isArmor(ItemStack s) {
            return s.getItem() instanceof ArmorItem;
        }

        private static boolean isBlock(ItemStack s) {
            return s.getItem() instanceof BlockItem;
        }

        private static boolean isFood(ItemStack s) {
            return s.has(DataComponents.FOOD);
        }
    }

    public enum Sort {
        NEWEST("Сначала новые"),
        PRICE_ASC("Дешевле"),
        PRICE_DESC("Дороже"),
        ENDING("Скоро истекут");

        public final String title;

        Sort(String title) {
            this.title = title;
        }

        public Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static List<Listing> query(MinecraftServer server, Category cat, Sort sort, UUID onlySeller) {
        List<Listing> out = new ArrayList<>();
        for (Listing l : AuctionStore.get(server).all()) {
            if (onlySeller != null && !l.seller.equals(onlySeller)) continue;
            if (!cat.matches(l.stack)) continue;
            out.add(l);
        }
        Comparator<Listing> cmp = switch (sort) {
            case NEWEST -> Comparator.comparingLong((Listing l) -> l.createdAt).reversed();
            case PRICE_ASC -> Comparator.comparingLong((Listing l) -> l.price);
            case PRICE_DESC -> Comparator.comparingLong((Listing l) -> l.price).reversed();
            case ENDING -> Comparator.comparingLong((Listing l) -> l.expiresAt);
        };
        out.sort(cmp);
        return out;
    }

    // ---------- операции ----------

    private static boolean blacklisted(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Arrays.stream(Config.AUCTION_BLACKLIST.get().split(","))
                .map(String::trim)
                .anyMatch(id::equalsIgnoreCase);
    }

    /** Выставить на аукцион предмет из основной руки. amount <= 0 — вся стопка. */
    public static void sell(ServerPlayer player, long price, int amount) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            Fmt.err(player, "Возьмите предмет, который хотите продать, в основную руку.");
            return;
        }
        long min = Config.AUCTION_MIN_PRICE.get();
        long max = Config.AUCTION_MAX_PRICE.get();
        if (price < min || price > max) {
            Fmt.err(player, "Цена должна быть от " + Fmt.coins(min) + " до " + Fmt.coins(max) + ".");
            return;
        }
        if (blacklisted(held)) {
            Fmt.err(player, "Этот предмет нельзя продавать на аукционе.");
            return;
        }
        AuctionStore store = AuctionStore.get(player.server);
        int limit = Config.AUCTION_MAX_LISTINGS.get();
        if (store.countBy(player.getUUID()) >= limit) {
            Fmt.err(player, "Достигнут лимит лотов: " + limit + ". Снимите или дождитесь продажи старых.");
            return;
        }
        int count = amount <= 0 ? held.getCount() : Math.min(amount, held.getCount());
        ItemStack sold = held.copyWithCount(count);
        held.shrink(count);

        long now = System.currentTimeMillis();
        long hours = Config.AUCTION_DURATION_HOURS.get();
        Listing l = new Listing(store.nextId(), player.getUUID(), player.getGameProfile().getName(),
                sold, price, now, now + hours * 3_600_000L);
        store.add(l);

        Fmt.ok(player, "Лот выставлен: " + sold.getHoverName().getString() + " x" + count
                + " за " + Fmt.coins(price) + " (на " + hours + " ч). Налог при продаже: "
                + Math.round(Config.AUCTION_TAX.get() * 100) + "%.");
    }

    public static boolean buy(ServerPlayer buyer, long listingId) {
        MinecraftServer server = buyer.server;
        AuctionStore store = AuctionStore.get(server);
        PlayerStore players = PlayerStore.get(server);

        Listing l = store.find(listingId);
        if (l == null) {
            Fmt.err(buyer, "Лот уже продан или снят с продажи.");
            return false;
        }
        if (l.seller.equals(buyer.getUUID())) {
            Fmt.err(buyer, "Нельзя купить собственный лот.");
            return false;
        }
        PlayerData bd = players.data(buyer);
        if (bd.balance < l.price) {
            Fmt.err(buyer, "Недостаточно средств. Нужно: " + Fmt.coins(l.price) + ", у вас: " + Fmt.coins(bd.balance));
            return false;
        }

        long tax = Math.round(l.price * Config.AUCTION_TAX.get());
        long net = l.price - tax;

        bd.balance -= l.price;
        PlayerData sd = players.data(l.seller);
        sd.balance = Fmt.satAdd(sd.balance, net);
        store.remove(l);

        String itemName = l.stack.getHoverName().getString() + " x" + l.stack.getCount();
        Inv.give(buyer, l.stack.copy());
        Fmt.ok(buyer, "Вы купили " + itemName + " за " + Fmt.coins(l.price) + ".");

        ServerPlayer seller = server.getPlayerList().getPlayer(l.seller);
        if (seller != null) {
            Fmt.ok(seller, "Ваш лот " + itemName + " куплен игроком " + buyer.getGameProfile().getName()
                    + ". Получено: " + Fmt.coins(net) + " (налог " + Fmt.coins(tax) + ").");
        } else {
            sd.offlineEarnings = Fmt.satAdd(sd.offlineEarnings, net);
            sd.offlineSales++;
        }
        players.setDirty();
        return true;
    }

    /** Снять свой лот: предмет возвращается игроку. */
    public static boolean cancel(ServerPlayer player, long listingId) {
        AuctionStore store = AuctionStore.get(player.server);
        Listing l = store.find(listingId);
        if (l == null) {
            Fmt.err(player, "Лот не найден.");
            return false;
        }
        if (!l.seller.equals(player.getUUID())) {
            Fmt.err(player, "Это не ваш лот.");
            return false;
        }
        store.remove(l);
        Inv.give(player, l.stack.copy());
        Fmt.ok(player, "Лот снят, предмет возвращён.");
        return true;
    }

    /** Забрать накопленные предметы из хранилища. */
    public static void claim(ServerPlayer player) {
        AuctionStore store = AuctionStore.get(player.server);
        List<ItemStack> list = store.claimsOf(player.getUUID());
        if (list.isEmpty()) {
            Fmt.info(player, "В хранилище ничего нет.");
            return;
        }
        int taken = 0;
        Iterator<ItemStack> it = list.iterator();
        while (it.hasNext()) {
            ItemStack s = it.next();
            player.getInventory().add(s);
            if (s.isEmpty()) {
                it.remove();
                taken++;
            }
        }
        store.touch();
        if (taken > 0) Fmt.ok(player, "Получено стопок: " + taken + ".");
        if (!list.isEmpty()) Fmt.info(player, "Инвентарь заполнен, в хранилище осталось стопок: " + list.size() + ".");
    }

    /** Переносит истёкшие лоты в хранилище продавцов. Вызывается раз в 30 секунд. */
    public static void expire(MinecraftServer server) {
        AuctionStore store = AuctionStore.get(server);
        long now = System.currentTimeMillis();
        List<Listing> expired = new ArrayList<>();
        for (Listing l : store.all()) {
            if (l.expiresAt <= now) expired.add(l);
        }
        for (Listing l : expired) {
            store.remove(l);
            store.addClaim(l.seller, l.stack.copy());
            ServerPlayer seller = server.getPlayerList().getPlayer(l.seller);
            if (seller != null) {
                seller.sendSystemMessage(Component.literal("Ваш лот " + l.stack.getHoverName().getString()
                        + " истёк. Заберите предмет: /ah claim").withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
