package com.example.servercore.menu;

import com.example.servercore.Config;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.pet.Pet;
import com.example.servercore.pet.PetManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Меню коллекции питомцев (6 рядов, 45 питомцев на странице). */
public final class PetsGui {
    private static final int PAGE = 45;

    private final ServerPlayer player;
    private final SimpleContainer c = new SimpleContainer(54);
    private int page;

    private PetsGui(ServerPlayer player) {
        this.player = player;
    }

    public static void open(ServerPlayer player) {
        new PetsGui(player).show();
    }

    private void show() {
        render();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new GuiMenu(id, inv, c, 6, this::click, null),
                Component.literal("Питомцы")));
    }

    private PlayerData data() {
        return PlayerStore.get(player.server).data(player);
    }

    private void render() {
        PlayerData d = data();
        int pages = Math.max(1, (d.pets.size() + PAGE - 1) / PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < PAGE; i++) {
            int idx = page * PAGE + i;
            c.setItem(i, idx < d.pets.size() ? petIcon(d.pets.get(idx), d) : ItemStack.EMPTY);
        }
        for (int i = 45; i < 54; i++) c.setItem(i, Gui.pane());

        Pet active = d.activePet();
        c.setItem(45, Gui.icon(Items.ARROW, "← Назад", ChatFormatting.YELLOW,
                List.of(Fmt.text("Страница " + (page + 1) + " / " + pages, ChatFormatting.GRAY))));
        c.setItem(47, Gui.icon(Items.EGG, "Купить яйцо питомца", ChatFormatting.GOLD,
                List.of(Fmt.text("Цена: " + Fmt.coins(Config.PET_EGG_PRICE.get()), ChatFormatting.YELLOW),
                        Fmt.text("Случайный тип и редкость", ChatFormatting.GRAY),
                        Fmt.text("Шанс легендарного: ~1%", ChatFormatting.GRAY))));
        c.setItem(49, Gui.icon(Items.NAME_TAG,
                "Питомцев: " + d.pets.size() + " / " + Config.PET_MAX_OWNED.get(), ChatFormatting.WHITE,
                List.of(Fmt.text(active == null ? "Активного питомца нет"
                        : "Активен: " + active.type.title + " [ур. " + active.level + "]", ChatFormatting.GRAY),
                        Component.empty(),
                        Fmt.text("ЛКМ по питомцу — выбрать / снять", ChatFormatting.GRAY),
                        Fmt.text("ПКМ по питомцу — превратить в предмет", ChatFormatting.GRAY),
                        Fmt.text("(предмет можно продать на аукционе)", ChatFormatting.DARK_GRAY))));
        c.setItem(51, Gui.icon(Items.BARRIER, "Снять активного питомца", ChatFormatting.RED, List.of()));
        c.setItem(53, Gui.icon(Items.ARROW, "Вперёд →", ChatFormatting.YELLOW,
                List.of(Fmt.text("Страница " + (page + 1) + " / " + pages, ChatFormatting.GRAY))));
    }

    private ItemStack petIcon(Pet pet, PlayerData d) {
        boolean active = pet.id.equals(d.activePet);
        List<Component> lore = new ArrayList<>(PetManager.lore(pet, active));
        lore.add(Component.empty());
        lore.add(Fmt.text(active ? "ЛКМ — снять питомца" : "ЛКМ — выбрать питомца", ChatFormatting.YELLOW));
        lore.add(Fmt.text("ПКМ — превратить в предмет", ChatFormatting.GRAY));
        ItemStack s = Gui.icon(pet.type.icon, Fmt.text(pet.type.title + " [ур. " + pet.level + "]", pet.rarity.color), lore);
        if (active) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    private void click(ServerPlayer p, int slot, int button, ClickType type) {
        PlayerData d = data();
        if (slot < PAGE) {
            int idx = page * PAGE + slot;
            if (idx >= d.pets.size()) return;
            Pet pet = d.pets.get(idx);
            if (button == 1) {
                UUID petId = pet.id;
                Gui.later(p, () -> ConfirmGui.open(p,
                        Component.literal("Превратить питомца в предмет?"), petIcon(pet, d),
                        () -> {
                            if (PetManager.extract(p, petId)) Fmt.ok(p, "Питомец превращён в предмет.");
                            show();
                        }, this::show));
                return;
            }
            if (pet.id.equals(d.activePet)) {
                PetManager.equip(p, null);
                Fmt.info(p, "Питомец снят.");
            } else {
                PetManager.equip(p, pet.id);
                Fmt.ok(p, "Активный питомец: " + pet.type.title + " [ур. " + pet.level + "]");
            }
        } else {
            switch (slot) {
                case 45 -> page = Math.max(0, page - 1);
                case 47 -> PetManager.buyEgg(p);
                case 51 -> {
                    PetManager.equip(p, null);
                    Fmt.info(p, "Питомец снят.");
                }
                case 53 -> page++;
                default -> {
                    return;
                }
            }
        }
        render();
    }
}
