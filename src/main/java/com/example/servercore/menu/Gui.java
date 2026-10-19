package com.example.servercore.menu;

import com.example.servercore.util.Fmt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** Помощники для построения серверных меню. */
public final class Gui {
    private Gui() {
    }

    public static ItemStack icon(Item item, Component name, List<Component> lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, name);
        if (!lore.isEmpty()) s.set(DataComponents.LORE, new ItemLore(lore));
        s.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(List.of(), false));
        s.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return s;
    }

    public static ItemStack icon(Item item, String name, ChatFormatting color, List<Component> lore) {
        return icon(item, Fmt.text(name, color), lore);
    }

    /** Серое стекло-заполнитель. */
    public static ItemStack pane() {
        return icon(Items.GRAY_STAINED_GLASS_PANE, Component.literal(" "), List.of());
    }

    /** Выполнить на следующем тике (безопасно открывать другое меню из обработчика клика). */
    public static void later(ServerPlayer player, Runnable action) {
        player.server.execute(action);
    }
}
