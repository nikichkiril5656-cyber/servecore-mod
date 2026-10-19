package com.example.servercore;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * ServerCore: аукцион, навыки и питомцы.
 * Мод полностью серверный: весь интерфейс построен на ванильных сундуках,
 * поэтому игрокам не нужно ничего устанавливать.
 */
@Mod(ServerCore.MODID)
public class ServerCore {
    public static final String MODID = "servercore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerCore(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("ServerCore загружен: аукцион, навыки, питомцы");
    }
}
