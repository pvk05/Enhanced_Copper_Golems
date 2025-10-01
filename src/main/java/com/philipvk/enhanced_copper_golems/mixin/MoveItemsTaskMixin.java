package com.philipvk.enhanced_copper_golems.mixin;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.ai.brain.task.MoveItemsTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(MoveItemsTask.class)
public abstract class MoveItemsTaskMixin {

    // Shadow the private methods from MoveItemsTask
    @Shadow
    protected abstract Box getSearchBoundingBox(PathAwareEntity entity);

    @Shadow
    private static Set<GlobalPos> getVisitedPositions(PathAwareEntity entity) {
        return null;
    }

    @Shadow
    private static Set<GlobalPos> getUnreachablePositions(PathAwareEntity entity) {
        return null;
    }

    @Shadow
    protected abstract int getHorizontalRange(PathAwareEntity entity);

    @Shadow
    protected abstract MoveItemsTask.Storage getStorageFor(
            PathAwareEntity entity,
            World world,
            BlockEntity blockEntity,
            Set<GlobalPos> visited,
            Set<GlobalPos> unreachable,
            Box box
    );

    @Inject(
            method = "findStorage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void allowMoreContainers(ServerWorld world, PathAwareEntity entity, CallbackInfoReturnable<Optional<MoveItemsTask.Storage>> cir) {
        Box box = this.getSearchBoundingBox(entity);
        Set<GlobalPos> visited = getVisitedPositions(entity);
        Set<GlobalPos> unreachable = getUnreachablePositions(entity);

        MoveItemsTask.Storage bestStorage = null;
        double bestDistance = Double.MAX_VALUE;

        // Iterate through nearby chunks
        List<ChunkPos> chunks = ChunkPos.stream(new ChunkPos(entity.getBlockPos()), Math.floorDiv(this.getHorizontalRange(entity), 16) + 1).toList();

        for (ChunkPos chunkPos : chunks) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
            if (chunk != null) {
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (isAllowedContainer(be)) {
                        double dist = be.getPos().getSquaredDistance(entity.getBlockPos());
                        if (dist < bestDistance) {
                            MoveItemsTask.Storage storage = this.getStorageFor(entity, world, be, visited, unreachable, box);
                            if (storage != null) {
                                bestStorage = storage;
                                bestDistance = dist;
                            }
                        }
                    }
                }
            }
        }

        if (bestStorage != null) {
            cir.setReturnValue(Optional.of(bestStorage));
            cir.cancel();
        }
    }

    @Unique
    private boolean isAllowedContainer(BlockEntity be) {
        return be instanceof ChestBlockEntity
                || be instanceof BarrelBlockEntity
                || be instanceof ShulkerBoxBlockEntity;
    }
}
