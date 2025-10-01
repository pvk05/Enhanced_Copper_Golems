package com.philipvk.enhanced_copper_golems;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Enhanced_copper_golems implements ModInitializer {
    public static final String MOD_ID = "enhanced_copper_golems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Mod");
    }
}
