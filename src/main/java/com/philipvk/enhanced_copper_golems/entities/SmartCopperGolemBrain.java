package com.philipvk.enhanced_copper_golems.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.philipvk.enhanced_copper_golems.Enhanced_copper_golems;
import com.philipvk.enhanced_copper_golems.entities.ai.SmartMoveItemsTask;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.FleeTask;
import net.minecraft.entity.ai.brain.task.LookAtMobWithIntervalTask;
import net.minecraft.entity.ai.brain.task.MoveToTargetTask;
import net.minecraft.entity.ai.brain.task.OpenDoorsTask;
import net.minecraft.entity.ai.brain.task.RandomTask;
import net.minecraft.entity.ai.brain.task.StrollTask;
import net.minecraft.entity.ai.brain.task.TickCooldownTask;
import net.minecraft.entity.ai.brain.task.UpdateLookControlTask;
import net.minecraft.entity.ai.brain.task.WaitTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.CopperGolemState;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import org.jetbrains.annotations.Nullable;

public class SmartCopperGolemBrain {
    private static final float FLEEING_SPEED = 1.5F;
    private static final float WALKING_SPEED = 1.0F;
    private static final int HORIZONTAL_RANGE = 32;
    private static final int VERTICAL_RANGE = 8;
    private static final int OPEN_INTERACTION_TICKS = 1;
    private static final int PLAY_SOUND_INTERACTION_TICKS = 9;
    private static final Predicate<BlockState> INPUT_CHEST_PREDICATE = state -> state.isIn(BlockTags.COPPER_CHESTS);
    private static final Predicate<BlockState> OUTPUT_CHEST_PREDICATE = SmartCopperGolemBrain::isValidOutput;

    public static boolean isValidOutput(BlockState state) {
        return (state.isOf(Blocks.CHEST) && Enhanced_copper_golems.CONFIG.chest())
                || (state.isOf(Blocks.TRAPPED_CHEST) && Enhanced_copper_golems.CONFIG.trappedChest())
                || (state.isOf(Blocks.BARREL) && Enhanced_copper_golems.CONFIG.barrel())
                || (state.getBlock() instanceof ShulkerBoxBlock && Enhanced_copper_golems.CONFIG.shulkerbox());
    }

    private static final ImmutableList<SensorType<? extends Sensor<? super SmartCopperGolemEntity>>> SENSORS = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY
    );
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
            MemoryModuleType.IS_PANICKING,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.MOBS,
            MemoryModuleType.VISIBLE_MOBS,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.GAZE_COOLDOWN_TICKS,
            MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS,
            MemoryModuleType.VISITED_BLOCK_POSITIONS,
            MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS,
            MemoryModuleType.DOORS_TO_CLOSE
    );

    public static Brain.Profile<SmartCopperGolemEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULES, SENSORS);
    }

    protected static Brain<?> create(Brain<SmartCopperGolemEntity> brain) {
        addCoreActivities(brain);
        addIdleActivities(brain);
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.resetPossibleActivities();
        return brain;
    }

    public static void updateActivity(SmartCopperGolemEntity entity) {
        entity.getBrain().resetPossibleActivities(ImmutableList.of(Activity.IDLE));
    }

    private static void addCoreActivities(Brain<SmartCopperGolemEntity> brain) {
        brain.setTaskList(
                Activity.CORE,
                0,
                ImmutableList.of(
                        new FleeTask<>(1.5F),
                        new UpdateLookControlTask(45, 90),
                        new MoveToTargetTask(),
                        OpenDoorsTask.create(),
                        new TickCooldownTask(MemoryModuleType.GAZE_COOLDOWN_TICKS),
                        new TickCooldownTask(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS)
                )
        );
    }

    private static void addIdleActivities(Brain<SmartCopperGolemEntity> brain) {
        brain.setTaskList(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(
                                0,
                                new SmartMoveItemsTask(
                                        1.0F, INPUT_CHEST_PREDICATE, OUTPUT_CHEST_PREDICATE, 32, 8, createInteractionCallbacks(), createResetToIdleCallback(), createStoragePredicate()
                                )
                        ),
                        Pair.of(1, LookAtMobWithIntervalTask.follow(EntityType.PLAYER, 6.0F, UniformIntProvider.create(40, 80))),
                        Pair.of(
                                2,
                                new RandomTask<>(
                                        ImmutableMap.of(
                                                MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT, MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, MemoryModuleState.VALUE_PRESENT
                                        ),
                                        ImmutableList.of(Pair.of(StrollTask.create(1.0F, 2, 2), 1), Pair.of(new WaitTask(30, 60), 1))
                                )
                        )
                )
        );
    }

    private static Map<SmartMoveItemsTask.InteractionState, SmartMoveItemsTask.InteractionCallback> createInteractionCallbacks() {
        return Map.of(
                SmartMoveItemsTask.InteractionState.PICKUP_ITEM,
                createInteractionCallback(CopperGolemState.GETTING_ITEM, SoundEvents.ENTITY_COPPER_GOLEM_NO_ITEM_GET),
                SmartMoveItemsTask.InteractionState.PICKUP_NO_ITEM,
                createInteractionCallback(CopperGolemState.GETTING_NO_ITEM, SoundEvents.ENTITY_COPPER_GOLEM_NO_ITEM_NO_GET),
                SmartMoveItemsTask.InteractionState.PLACE_ITEM,
                createInteractionCallback(CopperGolemState.DROPPING_ITEM, SoundEvents.ENTITY_COPPER_GOLEM_ITEM_DROP),
                SmartMoveItemsTask.InteractionState.PLACE_NO_ITEM,
                createInteractionCallback(CopperGolemState.DROPPING_NO_ITEM, SoundEvents.ENTITY_COPPER_GOLEM_ITEM_NO_DROP)
        );
    }

    private static SmartMoveItemsTask.InteractionCallback createInteractionCallback(CopperGolemState state, @Nullable SoundEvent soundEvent) {
        return (pathAwareEntity, storage, interactionTicks) -> {
            if (pathAwareEntity instanceof SmartCopperGolemEntity copperGolemEntity) {
                Inventory inventory = storage.inventory();
                if (interactionTicks == 1) {
                    inventory.onOpen(copperGolemEntity);
                    copperGolemEntity.setTargetContainerPos(storage.pos());
                    copperGolemEntity.setState(state);
                }

                if (interactionTicks == 9 && soundEvent != null) {
                    copperGolemEntity.playSoundIfNotSilent(soundEvent);
                }

                if (interactionTicks == 60) {
                    if (inventory.getViewingUsers().contains(pathAwareEntity)) {
                        inventory.onClose(copperGolemEntity);
                    }
                    else if (storage.blockEntity() instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
                        shulkerBoxBlockEntity.onClose(copperGolemEntity);
                    }

                    copperGolemEntity.resetTargetContainerPos();
                }
            }
        };
    }

    private static Consumer<PathAwareEntity> createResetToIdleCallback() {
        return entity -> {
            if (entity instanceof SmartCopperGolemEntity copperGolemEntity) {
                copperGolemEntity.resetTargetContainerPos();
                copperGolemEntity.setState(CopperGolemState.IDLE);
            }
        };
    }

    private static Predicate<SmartMoveItemsTask.Storage> createStoragePredicate() {
        return storage -> storage.blockEntity() instanceof ChestBlockEntity chestBlockEntity ? !chestBlockEntity.getViewingUsers().isEmpty() : false;
    }
}
