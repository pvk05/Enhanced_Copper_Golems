package com.philipvk.enhanced_copper_golems.entities;

import com.mojang.serialization.Dynamic;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CopperGolemStatueBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.block.entity.ViewerCountManager;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.TargetUtil;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CopperGolemOxidationLevels;
import net.minecraft.entity.passive.CopperGolemState;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class SmartCopperGolemEntity extends GolemEntity implements ContainerUser, Shearable {
    private static final long field_61257 = -2L;
    private static final long field_61258 = -1L;
    private static final int field_61259 = 504000;
    private static final int field_61273 = 552000;
    private static final int field_61274 = 200;
    private static final int field_61275 = 240;
    private static final float field_61260 = 10.0F;
    private static final float field_63113 = 0.0058F;
    private static final int field_63114 = 60;
    private static final int field_63115 = 100;
    private static final TrackedData<Oxidizable.OxidationLevel> OXIDATION_LEVEL = DataTracker.registerData(
            SmartCopperGolemEntity.class, TrackedDataHandlerRegistry.OXIDATION_LEVEL
    );
    private static final TrackedData<CopperGolemState> COPPER_GOLEM_STATE = DataTracker.registerData(
            SmartCopperGolemEntity.class, TrackedDataHandlerRegistry.COPPER_GOLEM_STATE
    );
    @Nullable
    private BlockPos targetContainer;
    @Nullable
    private UUID lastStruckLightning;
    private long nextOxidationAge = -1L;
    private int spinHeadTimer = 0;
    private final AnimationState spinHeadAnimationState = new AnimationState();
    private final AnimationState gettingItemAnimationState = new AnimationState();
    private final AnimationState gettingNoItemAnimationState = new AnimationState();
    private final AnimationState droppingItemAnimationState = new AnimationState();
    private final AnimationState droppingNoItemAnimationState = new AnimationState();
    public static final EquipmentSlot POPPY_SLOT = EquipmentSlot.SADDLE;

    public SmartCopperGolemEntity(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
        this.getNavigation().setMaxFollowRange(48.0F);
        this.getNavigation().setCanOpenDoors(true);
        this.setPersistent();
        this.setState(CopperGolemState.IDLE);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_OTHER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
        this.getBrain().remember(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, this.getRandom().nextBetweenExclusive(60, 100));
    }

    public static DefaultAttributeContainer.Builder createSmartCopperGolemAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.STEP_HEIGHT, 1.0)
                .add(EntityAttributes.MAX_HEALTH, 12.0);
    }

    public CopperGolemState getState() {
        return this.dataTracker.get(COPPER_GOLEM_STATE);
    }

    public void setState(CopperGolemState state) {
        this.dataTracker.set(COPPER_GOLEM_STATE, state);
    }

    public Oxidizable.OxidationLevel getOxidationLevel() {
        return this.dataTracker.get(OXIDATION_LEVEL);
    }

    public void setOxidationLevel(Oxidizable.OxidationLevel oxidationLevel) {
        this.dataTracker.set(OXIDATION_LEVEL, oxidationLevel);
    }

    public void setTargetContainerPos(BlockPos pos) {
        this.targetContainer = pos;
    }

    public void resetTargetContainerPos() {
        this.targetContainer = null;
    }

    public AnimationState getSpinHeadAnimationState() {
        return this.spinHeadAnimationState;
    }

    public AnimationState getGettingItemAnimationState() {
        return this.gettingItemAnimationState;
    }

    public AnimationState getGettingNoItemAnimationState() {
        return this.gettingNoItemAnimationState;
    }

    public AnimationState getDroppingItemAnimationState() {
        return this.droppingItemAnimationState;
    }

    public AnimationState getDroppingNoItemAnimationState() {
        return this.droppingNoItemAnimationState;
    }

    @Override
    protected Brain.Profile<SmartCopperGolemEntity> createBrainProfile() {
        return SmartCopperGolemBrain.createBrainProfile();
    }

    @Override
    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return SmartCopperGolemBrain.create(this.createBrainProfile().deserialize(dynamic));
    }

    @Override
    public Brain<SmartCopperGolemEntity> getBrain() {
        return (Brain<SmartCopperGolemEntity>)super.getBrain();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OXIDATION_LEVEL, Oxidizable.OxidationLevel.UNAFFECTED);
        builder.add(COPPER_GOLEM_STATE, CopperGolemState.IDLE);
    }

    @Override
    public void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putLong("next_weather_age", this.nextOxidationAge);
        view.put("weather_state", Oxidizable.OxidationLevel.CODEC, this.getOxidationLevel());
    }

    @Override
    public void readCustomData(ReadView view) {
        super.readCustomData(view);
        this.nextOxidationAge = view.getLong("next_weather_age", -1L);
        this.setOxidationLevel((Oxidizable.OxidationLevel)view.read("weather_state", Oxidizable.OxidationLevel.CODEC).orElse(Oxidizable.OxidationLevel.UNAFFECTED));
    }

    @Override
    protected void mobTick(ServerWorld world) {
        Profiler profiler = Profilers.get();
        profiler.push("smartCopperGolemBrain");
        this.getBrain().tick(world, this);
        profiler.pop();
        profiler.push("smartCopperGolemActivityUpdate");
        SmartCopperGolemBrain.updateActivity(this);
        profiler.pop();
        super.mobTick(world);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getEntityWorld().isClient()) {
            if (!this.isAiDisabled()) {
                this.clientTick();
            }
        } else {
            this.serverTick((ServerWorld)this.getEntityWorld(), this.getEntityWorld().getRandom(), this.getEntityWorld().getTime());
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isEmpty()) {
            ItemStack itemStack2 = this.getMainHandStack();
            if (!itemStack2.isEmpty()) {
                TargetUtil.give(this, itemStack2, player.getEntityPos());
                this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                return ActionResult.SUCCESS;
            }
        }

        World world = this.getEntityWorld();
        if (itemStack.isOf(Items.SHEARS) && this.isShearable()) {
            if (world instanceof ServerWorld serverWorld) {
                this.sheared(serverWorld, SoundCategory.PLAYERS, itemStack);
                this.emitGameEvent(GameEvent.SHEAR, player);
                itemStack.damage(1, player, hand);
            }

            return ActionResult.SUCCESS;
        } else if (world.isClient()) {
            return ActionResult.PASS;
        } else if (itemStack.isOf(Items.HONEYCOMB) && this.nextOxidationAge != -2L) {
            world.syncWorldEvent(this, WorldEvents.BLOCK_WAXED, this.getBlockPos(), 0);
            this.nextOxidationAge = -2L;
            this.eat(player, hand, itemStack);
            return ActionResult.SUCCESS_SERVER;
        } else if (itemStack.isIn(ItemTags.AXES) && this.nextOxidationAge == -2L) {
            world.playSoundFromEntity(null, this, SoundEvents.ITEM_AXE_SCRAPE, this.getSoundCategory(), 1.0F, 1.0F);
            world.syncWorldEvent(this, WorldEvents.WAX_REMOVED, this.getBlockPos(), 0);
            this.nextOxidationAge = -1L;
            itemStack.damage(1, player, hand.getEquipmentSlot());
            return ActionResult.SUCCESS_SERVER;
        } else {
            if (itemStack.isIn(ItemTags.AXES)) {
                Oxidizable.OxidationLevel oxidationLevel = this.getOxidationLevel();
                if (oxidationLevel != Oxidizable.OxidationLevel.UNAFFECTED) {
                    world.playSoundFromEntity(null, this, SoundEvents.ITEM_AXE_SCRAPE, this.getSoundCategory(), 1.0F, 1.0F);
                    world.syncWorldEvent(this, WorldEvents.BLOCK_SCRAPED, this.getBlockPos(), 0);
                    this.nextOxidationAge = -1L;
                    this.dataTracker.set(OXIDATION_LEVEL, oxidationLevel.getDecreased(), true);
                    itemStack.damage(1, player, hand.getEquipmentSlot());
                    return ActionResult.SUCCESS_SERVER;
                }
            }

            return super.interactMob(player, hand);
        }
    }

    private void serverTick(ServerWorld world, Random random, long timeOfDay) {
        if (this.nextOxidationAge != -2L) {
            if (this.nextOxidationAge == -1L) {
                this.nextOxidationAge = timeOfDay + random.nextBetween(504000, 552000);
            } else {
                Oxidizable.OxidationLevel oxidationLevel = this.dataTracker.get(OXIDATION_LEVEL);
                boolean bl = oxidationLevel.equals(Oxidizable.OxidationLevel.OXIDIZED);
                if (timeOfDay >= this.nextOxidationAge && !bl) {
                    Oxidizable.OxidationLevel oxidationLevel2 = oxidationLevel.getIncreased();
                    boolean bl2 = oxidationLevel2.equals(Oxidizable.OxidationLevel.OXIDIZED);
                    this.setOxidationLevel(oxidationLevel2);
                    this.nextOxidationAge = bl2 ? 0L : this.nextOxidationAge + random.nextBetween(504000, 552000);
                }

                if (bl && this.canTurnIntoStatue(world)) {
                    this.turnIntoStatue(world);
                }
            }
        }
    }

    private boolean canTurnIntoStatue(World world) {
        return world.getBlockState(this.getBlockPos()).isOf(Blocks.AIR) && world.random.nextFloat() <= 0.0058F;
    }

    private void turnIntoStatue(ServerWorld world) {
        BlockPos blockPos = this.getBlockPos();
        world.setBlockState(
                blockPos,
                Blocks.OXIDIZED_COPPER_GOLEM_STATUE
                        .getDefaultState()
                        .with(CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.values()[this.random.nextBetweenExclusive(0, CopperGolemStatueBlock.Pose.values().length)])
                        .with(CopperGolemStatueBlock.FACING, Direction.fromHorizontalDegrees(this.getYaw())),
                Block.NOTIFY_ALL
        );
        if (world.getBlockEntity(blockPos) instanceof SmartCopperGolemStatueBlockEntity copperGolemStatueBlockEntity) {
            copperGolemStatueBlockEntity.copyDataFrom(this);
            this.dropAllForeignEquipment(world);
            this.discard();
            this.playSoundIfNotSilent(SoundEvents.ENTITY_COPPER_GOLEM_BECOME_STATUE);
            if (this.isLeashed()) {
                if (world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                    this.detachLeash();
                } else {
                    this.detachLeashWithoutDrop();
                }
            }
        }
    }

    private void clientTick() {
        switch (this.getState()) {
            case IDLE:
                this.gettingNoItemAnimationState.stop();
                this.gettingItemAnimationState.stop();
                this.droppingItemAnimationState.stop();
                this.droppingNoItemAnimationState.stop();
                if (this.spinHeadTimer == this.age) {
                    this.spinHeadAnimationState.start(this.age);
                } else if (this.spinHeadTimer == 0) {
                    this.spinHeadTimer = this.age + this.random.nextBetweenExclusive(200, 240);
                }

                if (this.age == this.spinHeadTimer + 10.0F) {
                    this.playSpinHeadSound();
                    this.spinHeadTimer = 0;
                }
                break;
            case GETTING_ITEM:
                this.spinHeadAnimationState.stop();
                this.spinHeadTimer = 0;
                this.gettingNoItemAnimationState.stop();
                this.droppingItemAnimationState.stop();
                this.droppingNoItemAnimationState.stop();
                this.gettingItemAnimationState.startIfNotRunning(this.age);
                break;
            case GETTING_NO_ITEM:
                this.spinHeadAnimationState.stop();
                this.spinHeadTimer = 0;
                this.gettingItemAnimationState.stop();
                this.droppingNoItemAnimationState.stop();
                this.droppingItemAnimationState.stop();
                this.gettingNoItemAnimationState.startIfNotRunning(this.age);
                break;
            case DROPPING_ITEM:
                this.spinHeadAnimationState.stop();
                this.spinHeadTimer = 0;
                this.gettingItemAnimationState.stop();
                this.gettingNoItemAnimationState.stop();
                this.droppingNoItemAnimationState.stop();
                this.droppingItemAnimationState.startIfNotRunning(this.age);
                break;
            case DROPPING_NO_ITEM:
                this.spinHeadAnimationState.stop();
                this.spinHeadTimer = 0;
                this.gettingItemAnimationState.stop();
                this.gettingNoItemAnimationState.stop();
                this.droppingItemAnimationState.stop();
                this.droppingNoItemAnimationState.startIfNotRunning(this.age);
        }
    }

    public void onSpawn(Oxidizable.OxidationLevel oxidationLevel) {
        this.setOxidationLevel(oxidationLevel);
        this.playSpawnSound();
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        this.playSpawnSound();
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    public void playSpawnSound() {
        this.playSoundIfNotSilent(SoundEvents.ENTITY_COPPER_GOLEM_SPAWN);
    }

    private void playSpinHeadSound() {
        if (!this.isSilent()) {
            this.getEntityWorld().playSoundClient(this.getX(), this.getY(), this.getZ(), this.getSpinHeadSound(), this.getSoundCategory(), 1.0F, 1.0F, false);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return CopperGolemOxidationLevels.get(this.getOxidationLevel()).hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CopperGolemOxidationLevels.get(this.getOxidationLevel()).deathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(CopperGolemOxidationLevels.get(this.getOxidationLevel()).stepSound(), 1.0F, 1.0F);
    }

    private SoundEvent getSpinHeadSound() {
        return CopperGolemOxidationLevels.get(this.getOxidationLevel()).spinHeadSound();
    }

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0, 0.75F * this.getStandingEyeHeight(), 0.0);
    }

    @Override
    public boolean isViewingContainerAt(ViewerCountManager viewerCountManager, BlockPos pos) {
        if (this.targetContainer == null) {
            return false;
        } else {
            BlockState blockState = this.getEntityWorld().getBlockState(this.targetContainer);
            return this.targetContainer.equals(pos)
                    || blockState.getBlock() instanceof ChestBlock
                    && blockState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE
                    && ChestBlock.getPosInFrontOf(this.targetContainer, blockState).equals(pos);
        }
    }

    @Override
    public double getContainerInteractionRange() {
        return 3.0;
    }

    @Override
    public void sheared(ServerWorld world, SoundCategory shearedSoundCategory, ItemStack shears) {
        world.playSoundFromEntity(null, this, SoundEvents.ENTITY_COPPER_GOLEM_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
        ItemStack itemStack = this.getEquippedStack(POPPY_SLOT);
        this.equipStack(POPPY_SLOT, ItemStack.EMPTY);
        this.dropStack(world, itemStack, 1.5F);
    }

    @Override
    public boolean isShearable() {
        return this.isAlive() && this.getEquippedStack(POPPY_SLOT).isIn(ItemTags.SHEARABLE_FROM_COPPER_GOLEM);
    }

    @Override
    protected void dropInventory(ServerWorld world) {
        super.dropInventory(world);
        this.dropAllForeignEquipment(world);
    }

    @Override
    protected void applyDamage(ServerWorld world, DamageSource source, float amount) {
        super.applyDamage(world, source, amount);
        this.setState(CopperGolemState.IDLE);
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        super.onStruckByLightning(world, lightning);
        UUID uUID = lightning.getUuid();
        if (!uUID.equals(this.lastStruckLightning)) {
            this.lastStruckLightning = uUID;
            Oxidizable.OxidationLevel oxidationLevel = this.getOxidationLevel();
            if (oxidationLevel != Oxidizable.OxidationLevel.UNAFFECTED) {
                this.nextOxidationAge = -1L;
                this.dataTracker.set(OXIDATION_LEVEL, oxidationLevel.getDecreased(), true);
            }
        }
    }
}

