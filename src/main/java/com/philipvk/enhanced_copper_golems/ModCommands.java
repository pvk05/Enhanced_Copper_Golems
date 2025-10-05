package com.philipvk.enhanced_copper_golems;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class ModCommands {

    public static void initialize() {
        Enhanced_copper_golems.LOGGER.info("Initializing commands");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("test_command").executes(context -> {
                context.getSource().sendFeedback(() -> Text.literal("Called /test_command."), false);
                return 1;
            }));
        });
    }
}
