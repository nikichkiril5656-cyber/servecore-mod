package com.example.servercore.menu;

import com.example.servercore.Config;
import com.example.servercore.data.PlayerData;
import com.example.servercore.data.PlayerStore;
import com.example.servercore.skill.Skill;
import com.example.servercore.skill.SkillManager;
import com.example.servercore.util.Fmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Меню навыков (3 ряда). */
public final class SkillsGui {
    private static final int[] SLOTS = {11, 12, 13, 14, 15};

    private SkillsGui() {
    }

    public static void open(ServerPlayer player) {
        PlayerData d = PlayerStore.get(player.server).data(player);
        SimpleContainer c = new SimpleContainer(27);
        for (int i = 0; i < 27; i++) c.setItem(i, Gui.pane());

        Skill[] skills = Skill.values();
        for (int i = 0; i < skills.length && i < SLOTS.length; i++) {
            c.setItem(SLOTS[i], skillIcon(skills[i], d));
        }
        c.setItem(21, Gui.icon(Items.TURTLE_EGG, "Питомцы", ChatFormatting.LIGHT_PURPLE,
                List.of(Fmt.text("Нажмите, чтобы открыть", ChatFormatting.GRAY))));
        c.setItem(22, Gui.icon(Items.GOLD_INGOT, "Баланс: " + Fmt.coins(d.balance), ChatFormatting.GOLD, List.of()));
        c.setItem(23, Gui.icon(Items.EMERALD, "Аукцион", ChatFormatting.GREEN,
                List.of(Fmt.text("Нажмите, чтобы открыть", ChatFormatting.GRAY))));

        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GuiMenu(id, inv, c, 3, (pl, slot, button, type) -> {
            if (slot == 21) Gui.later(pl, () -> PetsGui.open(pl));
            else if (slot == 23) Gui.later(pl, () -> AuctionGui.open(pl, false));
        }, null), Component.literal("Навыки")));
    }

    private static ItemStack skillIcon(Skill s, PlayerData d) {
        long xp = d.xp(s);
        int max = Config.SKILL_MAX_LEVEL.get();
        int lvl = SkillManager.levelOf(xp);
        List<Component> lore = new ArrayList<>();
        lore.add(Fmt.text("Уровень: " + lvl + " / " + max, ChatFormatting.GRAY));
        if (lvl < max) {
            long into = SkillManager.xpIntoLevel(xp);
            long need = SkillManager.xpForNext(lvl);
            lore.add(Fmt.bar(into / (double) need, 24));
            lore.add(Fmt.text(Fmt.num(into) + " / " + Fmt.num(need) + " опыта", ChatFormatting.DARK_GRAY));
        } else {
            lore.add(Fmt.text("МАКСИМАЛЬНЫЙ УРОВЕНЬ", ChatFormatting.GOLD));
        }
        lore.add(Component.empty());
        lore.add(Fmt.text("Бонусы за уровень:", ChatFormatting.GRAY));
        lore.add(Fmt.text(" " + s.bonusText, ChatFormatting.GREEN));
        lore.add(Component.empty());
        lore.add(Fmt.text("Всего опыта: " + Fmt.num(xp), ChatFormatting.DARK_GRAY));
        return Gui.icon(s.icon, s.title, s.color, lore);
    }
}
