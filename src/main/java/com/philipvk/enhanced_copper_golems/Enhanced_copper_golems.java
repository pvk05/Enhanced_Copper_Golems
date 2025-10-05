package com.philipvk.enhanced_copper_golems;

import com.philipvk.enhanced_copper_golems.entities.SmartCopperGolemEntity;
import com.philipvk.enhanced_copper_golems.events.TransformCopperGolem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.philipvk.enhanced_copper_golems.MyConfig;

public class Enhanced_copper_golems implements ModInitializer {
    public static final String MOD_ID = "enhanced_copper_golems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final MyConfig CONFIG = MyConfig.createAndLoad();

    public static final EntityType<SmartCopperGolemEntity> SMART_COPPER_GOLEM = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "smart_copper_golem"),
            EntityType.Builder.create(SmartCopperGolemEntity::new, SpawnGroup.MISC).dimensions(0.7F, 1.9F).build(RegistryKey.of(
                    Registries.ENTITY_TYPE.getKey(),
                    Identifier.of(MOD_ID, "smart_copper_golem")
            ))
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Mod");

        ModCommands.initialize();
        TransformCopperGolem.initialize();

        FabricDefaultAttributeRegistry.register(SMART_COPPER_GOLEM, SmartCopperGolemEntity.createSmartCopperGolemAttributes());
    }
}
