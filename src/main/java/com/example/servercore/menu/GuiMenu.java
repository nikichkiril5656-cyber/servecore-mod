package com.example.servercore.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Серверное меню на основе ванильного сундука. Предметы забрать нельзя,
 * все клики по слотам меню уходят в обработчик.
 */
public class GuiMenu extends ChestMenu {
    @FunctionalInterface
    public interface Handler {
        void click(ServerPlayer player, int slot, int button, ClickType type);
    }

    private final int guiSize;
    private final Handler handler;
    private final Consumer<ServerPlayer> onClose;

    public GuiMenu(int id, Inventory inventory, Container container, int rows, Handler handler,
                   Consumer<ServerPlayer> onClose) {
        super(typeFor(rows), id, inventory, container, rows);
        this.guiSize = rows * 9;
        this.handler = handler;
        this.onClose = onClose;
    }

    private static MenuType<?> typeFor(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player instanceof ServerPlayer sp && slotId >= 0 && slotId < guiSize
                && clickType != ClickType.QUICK_CRAFT) {
            handler.click(sp, slotId, button, clickType);
        }
        // Ничего не перемещаем: просто синхронизируем клиент с сервером.
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (onClose != null && player instanceof ServerPlayer sp) onClose.accept(sp);
    }
}
