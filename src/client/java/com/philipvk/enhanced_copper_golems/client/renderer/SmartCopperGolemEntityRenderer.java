package com.philipvk.enhanced_copper_golems.client.renderer;

import com.philipvk.enhanced_copper_golems.Enhanced_copper_golems;
import com.philipvk.enhanced_copper_golems.entities.SmartCopperGolemEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.CopperGolemHeadBlockFeatureRenderer;
import net.minecraft.client.render.entity.feature.EmissiveFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.CopperGolemEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.CopperGolemEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.passive.CopperGolemOxidationLevels;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class SmartCopperGolemEntityRenderer extends MobEntityRenderer<SmartCopperGolemEntity, CopperGolemEntityRenderState, CopperGolemEntityModel> {
    public SmartCopperGolemEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new CopperGolemEntityModel(context.getPart(EntityModelLayers.COPPER_GOLEM)), 0.5F);
        this.addFeature(
                new EmissiveFeatureRenderer<>(
                        this,
                        getEyeTextureGetter(),
                        (state, tickProgress) -> 1.0F,
                        new CopperGolemEntityModel(context.getPart(EntityModelLayers.COPPER_GOLEM)),
                        RenderLayer::getEyes,
                        false
                )
        );
        this.addFeature(new HeldItemFeatureRenderer<>(this));
        this.addFeature(new CopperGolemHeadBlockFeatureRenderer<>(this, state -> state.headBlockItemStack, this.model::transformMatricesForBlock));
        this.addFeature(new HeadFeatureRenderer<>(this, context.getEntityModels(), context.getPlayerSkinCache()));
    }

    public Identifier getTexture(CopperGolemEntityRenderState copperGolemEntityRenderState) {
        return Identifier.of(Enhanced_copper_golems.MOD_ID, "textures/entity/smart_copper_golem/smart_copper_golem.png");
    }

    private static Function<CopperGolemEntityRenderState, Identifier> getEyeTextureGetter() {
        return state -> CopperGolemOxidationLevels.get(state.oxidationLevel).eyeTexture();
    }

    public CopperGolemEntityRenderState createRenderState() {
        return new CopperGolemEntityRenderState();
    }

    public void updateRenderState(SmartCopperGolemEntity copperGolemEntity, CopperGolemEntityRenderState copperGolemEntityRenderState, float f) {
        super.updateRenderState(copperGolemEntity, copperGolemEntityRenderState, f);
        ArmedEntityRenderState.updateRenderState(copperGolemEntity, copperGolemEntityRenderState, this.itemModelResolver);
        copperGolemEntityRenderState.oxidationLevel = copperGolemEntity.getOxidationLevel();
        copperGolemEntityRenderState.copperGolemState = copperGolemEntity.getState();
        copperGolemEntityRenderState.spinHeadAnimationState.copyFrom(copperGolemEntity.getSpinHeadAnimationState());
        copperGolemEntityRenderState.gettingItemAnimationState.copyFrom(copperGolemEntity.getGettingItemAnimationState());
        copperGolemEntityRenderState.gettingNoItemAnimationState.copyFrom(copperGolemEntity.getGettingNoItemAnimationState());
        copperGolemEntityRenderState.droppingItemAnimationState.copyFrom(copperGolemEntity.getDroppingItemAnimationState());
        copperGolemEntityRenderState.droppingNoItemAnimationState.copyFrom(copperGolemEntity.getDroppingNoItemAnimationState());
        copperGolemEntityRenderState.headBlockItemStack = Optional.of(copperGolemEntity.getEquippedStack(SmartCopperGolemEntity.POPPY_SLOT)).flatMap(stack -> {
            if (stack.getItem() instanceof BlockItem blockItem) {
                BlockStateComponent blockStateComponent = (BlockStateComponent)stack.getOrDefault(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT);
                return Optional.of(blockStateComponent.applyToState(blockItem.getBlock().getDefaultState()));
            } else {
                return Optional.empty();
            }
        });
    }
}
