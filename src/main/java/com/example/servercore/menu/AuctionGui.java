package com.example.servercore.menu;

import com.example.servercore.auction.AuctionManager;
import com.example.servercore.auction.AuctionManager.Category;
import com.example.servercore.auction.AuctionManager.Sort;
import com.example.servercore.data.AuctionStore;
import com.example.servercore.data.Listing;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.util.Fmt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Главное меню аукциона (6 рядов): 45 лотов на странице и нижняя панель управления. */
public final class AuctionGui {
    private static final int PAGE = 45;

    private final ServerPlayer player;
    private final SimpleContainer c = new SimpleContainer(54);
    private int page;
    private Category cat = Category.ALL;
    private Sort sort = Sort.NEWEST;
    private boolean mine;
    private List<Listing> view = List.of();

    private AuctionGui(ServerPlayer player, boolean mine) {
        this.player = player;
        this.mine = mine;
    }

    public static void open(ServerPlayer player, boolean mine) {
        new AuctionGui(player, mine).show();
    }

    private void show() {
        render();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new GuiMenu(id, inv, c, 6, this::click, null),
                Component.literal(mine ? "Мои лоты" : "Аукцион")));
    }

    private void render() {
        view = AuctionManager.query(player.server, cat, sort, mine ? player.getUUID() : null);
        int pages = Math.max(1, (view.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        long now = System.currentTimeMillis();

        for (int i = 0; i < PAGE; i++) {
            int idx = page * PAGE + i;
            c.setItem(i, idx < view.size() ? listingIcon(view.get(idx), now) : ItemStack.EMPTY);
        }
        for (int i = 45; i < 54; i++) c.setItem(i, Gui.pane());

        UUID me = player.getUUID();
        long balance = PlayerStore.get(player.server).data(player).balance;
        int claims = AuctionStore.get(player.server).claimCount(me);

        c.setItem(45, Gui.icon(Items.ARROW, "← Назад", ChatFormatting.YELLOW,
                List.of(Fmt.text("Страница " + (page + 1) + " / " + pages, ChatFormatting.GRAY))));
        c.setItem(46, Gui.icon(Items.COMPASS, "Категория: " + cat.title, ChatFormatting.AQUA,
                List.of(Fmt.text("Нажмите, чтобы сменить", ChatFormatting.GRAY))));
        c.setItem(47, Gui.icon(Items.HOPPER, "Сортировка: " + sort.title, ChatFormatting.AQUA,
                List.of(Fmt.text("Нажмите, чтобы сменить", ChatFormatting.GRAY))));
        c.setItem(48, Gui.icon(mine ? Items.CHEST : Items.ENDER_CHEST,
                mine ? "Показать все лоты" : "Мои лоты", ChatFormatting.GOLD,
                List.of(Fmt.text("Ваших лотов: " + AuctionStore.get(player.server).countBy(me), ChatFormatting.GRAY),
                        Fmt.text(mine ? "ЛКМ по лоту — снять с продажи" : "Нажмите, чтобы переключить", ChatFormatting.GRAY))));
        c.setItem(49, Gui.icon(Items.GOLD_INGOT, "Баланс: " + Fmt.coins(balance), ChatFormatting.GOLD,
                List.of(Fmt.text("Лотов найдено: " + view.size(), ChatFormatting.GRAY))));
        c.setItem(50, Gui.icon(Items.BARREL, "Хранилище: " + claims, ChatFormatting.GREEN,
                List.of(Fmt.text("Истёкшие и возвращённые предметы", ChatFormatting.GRAY),
                        Fmt.text("Нажмите, чтобы забрать", ChatFormatting.YELLOW))));
        c.setItem(51, Gui.icon(Items.BOOK, "Как продать предмет", ChatFormatting.WHITE,
                List.of(Fmt.text("Возьмите предмет в руку и введите:", ChatFormatting.GRAY),
                        Fmt.text("/ah sell <цена> [количество]", ChatFormatting.YELLOW),
                        Fmt.text("Налог с продажи: " + Math.round(com.example.servercore.Config.AUCTION_TAX.get() * 100) + "%",
                                ChatFormatting.GRAY))));
        c.setItem(53, Gui.icon(Items.ARROW, "Вперёд →", ChatFormatting.YELLOW,
                List.of(Fmt.text("Страница " + (page + 1) + " / " + pages, ChatFormatting.GRAY))));
    }

    private ItemStack listingIcon(Listing l, long now) {
        ItemStack d = l.stack.copy();
        List<Component> lore = new ArrayList<>(d.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        lore.add(Component.empty());
        lore.add(Fmt.text("Цена: " + Fmt.coins(l.price), ChatFormatting.GOLD));
        lore.add(Fmt.text("Продавец: " + l.sellerName, ChatFormatting.GRAY));
        lore.add(Fmt.text("Осталось: " + Fmt.duration(l.expiresAt - now), ChatFormatting.GRAY));
        lore.add(Component.empty());
        if (mine) {
            lore.add(Fmt.text("ЛКМ — снять с продажи", ChatFormatting.RED));
        } else if (l.seller.equals(player.getUUID())) {
            lore.add(Fmt.text("Это ваш лот", ChatFormatting.DARK_GRAY));
        } else {
            lore.add(Fmt.text("ЛКМ — купить", ChatFormatting.GREEN));
        }
        d.set(DataComponents.LORE, new ItemLore(lore));
        return d;
    }

    private void click(ServerPlayer p, int slot, int button, ClickType type) {
        if (slot < PAGE) {
            int idx = page * PAGE + slot;
            if (idx >= view.size()) return;
            Listing l = view.get(idx);
            if (mine) {
                AuctionManager.cancel(p, l.id);
            } else if (l.seller.equals(p.getUUID())) {
                Fmt.info(p, "Это ваш лот. Чтобы снять его, откройте «Мои лоты».");
            } else {
                long id = l.id;
                ItemStack preview = listingIcon(l, System.currentTimeMillis());
                Gui.later(p, () -> ConfirmGui.open(p, Component.literal("Купить за " + Fmt.coins(l.price) + "?"), preview,
                        () -> {
                            AuctionManager.buy(p, id);
                            show();
                        }, this::show));
                return;
            }
        } else {
            switch (slot) {
                case 45 -> page = Math.max(0, page - 1);
                case 46 -> {
                    cat = cat.next();
                    page = 0;
                }
                case 47 -> {
                    sort = sort.next();
                    page = 0;
                }
                case 48 -> {
                    mine = !mine;
                    page = 0;
                    Gui.later(p, this::show); // заголовок меняется, поэтому открываем заново
                    return;
                }
                case 50 -> AuctionManager.claim(p);
                case 53 -> page++;
                default -> {
                    return;
                }
            }
        }
        render();
    }
}
