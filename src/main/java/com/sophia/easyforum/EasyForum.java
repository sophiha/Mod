package com.sophia.easyforum;

import net.weavemc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;

public class EasyForum implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("EasyForum");

    @Override
    public void init() {
        LOGGER.info("[EasyForum] Inicializando...");
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @Override
    public void preInit(@NotNull Instrumentation instrumentation) {}
}
