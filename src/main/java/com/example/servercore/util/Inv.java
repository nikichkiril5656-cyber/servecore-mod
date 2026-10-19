package com.example.servercore.util;

import com.example.servercore.data.AuctionStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Работа с инвентарём игрока. */
public final class Inv {
    private Inv() {
    }

    /**
     * Кладёт предмет в инвентарь. То, что не влезло, отправляется в хранилище
     * игрока (забрать через /ah claim), поэтому предметы никогда не теряются.
     */
    public static void give(ServerPlayer player, ItemStack stack) {
        ItemStack rest = stack.copy();
        player.getInventory().add(rest);
        if (!rest.isEmpty()) {
            AuctionStore.get(player.server).addClaim(player.getUUID(), rest);
            Fmt.info(player, "Инвентарь полон. Часть предметов ждёт в хранилище: /ah claim");
        }
    }
}
