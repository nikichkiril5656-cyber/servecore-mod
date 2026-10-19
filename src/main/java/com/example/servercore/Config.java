package com.example.servercore;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Все настройки мода. Файл: config/servercore-common.toml */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ---- Экономика ----
    public static final ModConfigSpec.ConfigValue<String> CURRENCY;
    public static final ModConfigSpec.LongValue START_BALANCE;

    // ---- Аукцион ----
    public static final ModConfigSpec.DoubleValue AUCTION_TAX;
    public static final ModConfigSpec.IntValue AUCTION_MAX_LISTINGS;
    public static final ModConfigSpec.IntValue AUCTION_DURATION_HOURS;
    public static final ModConfigSpec.LongValue AUCTION_MIN_PRICE;
    public static final ModConfigSpec.LongValue AUCTION_MAX_PRICE;
    public static final ModConfigSpec.ConfigValue<String> AUCTION_BLACKLIST;

    // ---- Навыки ----
    public static final ModConfigSpec.IntValue SKILL_MAX_LEVEL;
    public static final ModConfigSpec.DoubleValue SKILL_XP_MULT;
    public static final ModConfigSpec.LongValue SKILL_LEVEL_REWARD;

    // ---- Питомцы ----
    public static final ModConfigSpec.IntValue PET_MAX_OWNED;
    public static final ModConfigSpec.DoubleValue PET_XP_SHARE;
    public static final ModConfigSpec.DoubleValue PET_DROP_KILL;
    public static final ModConfigSpec.DoubleValue PET_DROP_FISH;
    public static final ModConfigSpec.LongValue PET_EGG_PRICE;

    public static final ModConfigSpec SPEC;

    static {
        B.push("economy");
        CURRENCY = B.comment("Символ валюты, показывается после суммы")
                .define("currencySymbol", "$");
        START_BALANCE = B.comment("Стартовый баланс нового игрока")
                .defineInRange("startBalance", 1000L, 0L, Long.MAX_VALUE);
        B.pop();

        B.push("auction");
        AUCTION_TAX = B.comment("Налог с продажи (0.05 = 5%). Забирается у продавца и исчезает из экономики")
                .defineInRange("tax", 0.05, 0.0, 1.0);
        AUCTION_MAX_LISTINGS = B.comment("Максимум одновременных лотов у одного игрока")
                .defineInRange("maxListingsPerPlayer", 10, 1, 100);
        AUCTION_DURATION_HOURS = B.comment("Сколько часов живёт лот, после чего предмет уходит в хранилище продавца")
                .defineInRange("durationHours", 48, 1, 24 * 30);
        AUCTION_MIN_PRICE = B.comment("Минимальная цена лота")
                .defineInRange("minPrice", 1L, 1L, Long.MAX_VALUE);
        AUCTION_MAX_PRICE = B.comment("Максимальная цена лота")
                .defineInRange("maxPrice", 1_000_000_000L, 1L, Long.MAX_VALUE);
        AUCTION_BLACKLIST = B.comment("Запрещённые к продаже предметы через запятую, например: minecraft:bedrock,minecraft:barrier")
                .define("blacklist", "minecraft:bedrock,minecraft:barrier,minecraft:command_block,minecraft:debug_stick");
        B.pop();

        B.push("skills");
        SKILL_MAX_LEVEL = B.comment("Максимальный уровень навыка")
                .defineInRange("maxLevel", 60, 1, 100);
        SKILL_XP_MULT = B.comment("Глобальный множитель опыта навыков")
                .defineInRange("xpMultiplier", 1.0, 0.0, 1000.0);
        SKILL_LEVEL_REWARD = B.comment("Награда монетами за новый уровень: уровень * это значение")
                .defineInRange("levelRewardPerLevel", 100L, 0L, 1_000_000L);
        B.pop();

        B.push("pets");
        PET_MAX_OWNED = B.comment("Сколько питомцев игрок может хранить")
                .defineInRange("maxOwned", 36, 1, 500);
        PET_XP_SHARE = B.comment("Какую долю заработанного опыта навыков получает активный питомец (1.0 = 100%)")
                .defineInRange("xpShare", 1.0, 0.0, 100.0);
        PET_DROP_KILL = B.comment("Базовый шанс выпадения питомца при убийстве враждебного моба (0.004 = 0.4%). У боссов шанс выше")
                .defineInRange("dropChanceKill", 0.004, 0.0, 1.0);
        PET_DROP_FISH = B.comment("Шанс питомца при рыбалке")
                .defineInRange("dropChanceFishing", 0.01, 0.0, 1.0);
        PET_EGG_PRICE = B.comment("Цена яйца питомца в команде /pets buy (случайный тип и редкость)")
                .defineInRange("eggPrice", 5000L, 0L, Long.MAX_VALUE);
        B.pop();

        SPEC = B.build();
    }

    private Config() {
    }
}
