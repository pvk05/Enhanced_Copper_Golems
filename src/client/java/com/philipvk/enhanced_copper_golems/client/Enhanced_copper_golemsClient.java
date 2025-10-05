package com.philipvk.enhanced_copper_golems.client;

import com.philipvk.enhanced_copper_golems.Enhanced_copper_golems;
import com.philipvk.enhanced_copper_golems.client.renderer.SmartCopperGolemEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.model.CopperGolemEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class Enhanced_copper_golemsClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_SMART_COPPER_GOLEM_LAYER = new EntityModelLayer(Identifier.of(Enhanced_copper_golems.MOD_ID, "smart_copper_golem"), "main");

    @Override
    public void onInitializeClient() {
        EntityRendererFactories.register(Enhanced_copper_golems.SMART_COPPER_GOLEM, SmartCopperGolemEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MODEL_SMART_COPPER_GOLEM_LAYER, CopperGolemEntityModel::getTexturedModelData);
    }
}
