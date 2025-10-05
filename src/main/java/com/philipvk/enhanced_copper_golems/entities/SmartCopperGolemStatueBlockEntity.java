package com.philipvk.enhanced_copper_golems.entities;

import com.philipvk.enhanced_copper_golems.Enhanced_copper_golems;
import net.minecraft.block.BlockState;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class SmartCopperGolemStatueBlockEntity extends BlockEntity {
    public SmartCopperGolemStatueBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.COPPER_GOLEM_STATUE, pos, state);
    }

    public void copyDataFrom(SmartCopperGolemEntity copperGolemEntity) {
        this.setComponents(ComponentMap.builder().addAll(this.getComponents()).add(DataComponentTypes.CUSTOM_NAME, copperGolemEntity.getCustomName()).build());
        super.markDirty();
    }

    @Nullable
    public SmartCopperGolemEntity createCopperGolem(BlockState state) {
        SmartCopperGolemEntity copperGolemEntity = Enhanced_copper_golems.SMART_COPPER_GOLEM.create(this.world, SpawnReason.TRIGGERED);
        if (copperGolemEntity != null) {
            copperGolemEntity.setCustomName(this.getComponents().get(DataComponentTypes.CUSTOM_NAME));
            return this.setupEntity(state, copperGolemEntity);
        } else {
            return null;
        }
    }

    private SmartCopperGolemEntity setupEntity(BlockState state, SmartCopperGolemEntity entity) {
        BlockPos blockPos = this.getPos();
        entity.refreshPositionAndAngles(
                blockPos.toCenterPos().x,
                blockPos.getY(),
                blockPos.toCenterPos().z,
                ((Direction)state.get(CopperGolemStatueBlock.FACING)).getPositiveHorizontalDegrees(),
                0.0F
        );
        entity.headYaw = entity.getYaw();
        entity.bodyYaw = entity.getYaw();
        entity.playSpawnSound();
        return entity;
    }

    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public ItemStack withComponents(ItemStack stack, CopperGolemStatueBlock.Pose pose) {
        stack.applyComponentsFrom(this.createComponentMap());
        stack.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT.with(CopperGolemStatueBlock.POSE, pose));
        return stack;
    }
}
