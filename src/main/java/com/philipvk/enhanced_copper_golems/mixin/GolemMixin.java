package com.philipvk.enhanced_copper_golems.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.passive.CopperGolemBrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(CopperGolemBrain.class)
public class GolemMixin {

    // Replace the 3rd argument (index = 2) to MoveItemsTask constructor
    @ModifyArg(
            method = "addIdleActivities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/task/MoveItemsTask;<init>(F"
                            + "Ljava/util/function/Predicate;"
                            + "Ljava/util/function/Predicate;"
                            + "IILjava/util/Map;"
                            + "Ljava/util/function/Consumer;"
                            + "Ljava/util/function/Predicate;)V"
            ),
            index = 2 // 0 = float speed, 1 = input predicate, 2 = output predicate
    )
    private static Predicate<BlockState> replaceOutputPredicate(Predicate<BlockState> original) {
        return state -> (
                state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.BARREL) || state.getBlock() instanceof ShulkerBoxBlock
        );
    }
}
