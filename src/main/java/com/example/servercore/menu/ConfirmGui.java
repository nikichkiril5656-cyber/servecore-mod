package com.example.servercore.menu;

import com.example.servercore.util.Fmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Меню подтверждения действия. */
public final class ConfirmGui {
    private ConfirmGui() {
    }

    public static void open(ServerPlayer player, Component title, ItemStack preview,
                            Runnable onConfirm, Runnable onCancel) {
        SimpleContainer c = new SimpleContainer(27);
        for (int i = 0; i < 27; i++) c.setItem(i, Gui.pane());
        c.setItem(13, preview);
        c.setItem(11, Gui.icon(Items.LIME_CONCRETE, "ПОДТВЕРДИТЬ", ChatFormatting.GREEN, List.of()));
        c.setItem(15, Gui.icon(Items.RED_CONCRETE, "ОТМЕНА", ChatFormatting.RED, List.of()));

        boolean[] used = {false};
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GuiMenu(id, inv, c, 3, (pl, slot, button, type) -> {
            if (used[0]) return;
            if (slot == 11) {
                used[0] = true;
                Gui.later(pl, onConfirm);
            } else if (slot == 15) {
                used[0] = true;
                Gui.later(pl, onCancel);
            }
        }, null), title));
    }

    public static ItemStack label(String text) {
        return Gui.icon(Items.PAPER, Fmt.text(text, ChatFormatting.YELLOW), List.of());
    }
}
