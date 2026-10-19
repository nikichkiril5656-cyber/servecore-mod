package com.example.servercore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Запоминает блоки, поставленные игроками, чтобы нельзя было фармить опыт,
 * ставя и ломая руду/брёвна. Хранится в памяти (до перезапуска) и ограничен по размеру.
 */
public final class PlacedBlocks {
    private static final int MAX = 100_000;
    private static final Map<String, Set<Long>> MAP = new HashMap<>();

    private PlacedBlocks() {
    }

    private static Set<Long> set(Level level) {
        String key = level.dimension().location().toString();
        return MAP.computeIfAbsent(key, k -> Collections.newSetFromMap(new LinkedHashMap<Long, Boolean>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                return size() > MAX;
            }
        }));
    }

    public static void mark(Level level, BlockPos pos) {
        set(level).add(pos.asLong());
    }

    /** true, если блок был поставлен игроком (метка при этом снимается). */
    public static boolean consume(Level level, BlockPos pos) {
        return set(level).remove(pos.asLong());
    }
}
