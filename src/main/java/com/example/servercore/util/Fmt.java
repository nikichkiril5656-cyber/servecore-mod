package com.example.servercore.util;

import com.example.servercore.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/** Форматирование чисел и текста. */
public final class Fmt {
    private Fmt() {
    }

    public static String num(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', ' ');
    }

    public static String coins(long n) {
        return num(n) + " " + Config.CURRENCY.get();
    }

    public static String duration(long ms) {
        if (ms <= 0) return "истёк";
        long m = ms / 60_000L;
        long h = m / 60;
        long d = h / 24;
        if (d > 0) return d + "д " + (h % 24) + "ч";
        if (h > 0) return h + "ч " + (m % 60) + "м";
        return Math.max(1, m) + "м";
    }

    /** Текст для лора/названий предметов: без курсива по умолчанию. */
    public static MutableComponent text(String s, ChatFormatting color) {
        return Component.literal(s).withStyle(color).withStyle(st -> st.withItalic(false));
    }

    /** Полоска прогресса. */
    public static MutableComponent bar(double fraction, int len) {
        double f = Math.max(0, Math.min(1, fraction));
        int filled = (int) Math.round(f * len);
        MutableComponent c = Component.literal("|".repeat(filled)).withStyle(ChatFormatting.GREEN);
        c.append(Component.literal("|".repeat(len - filled)).withStyle(ChatFormatting.DARK_GRAY));
        return c.withStyle(st -> st.withItalic(false));
    }

    public static void send(ServerPlayer p, ChatFormatting color, String message) {
        p.sendSystemMessage(Component.literal(message).withStyle(color));
    }

    public static void ok(ServerPlayer p, String message) {
        send(p, ChatFormatting.GREEN, message);
    }

    public static void err(ServerPlayer p, String message) {
        send(p, ChatFormatting.RED, message);
    }

    public static void info(ServerPlayer p, String message) {
        send(p, ChatFormatting.YELLOW, message);
    }

    /** Сумма с защитой от переполнения. */
    public static long satAdd(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) return b > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        return r;
    }
}
