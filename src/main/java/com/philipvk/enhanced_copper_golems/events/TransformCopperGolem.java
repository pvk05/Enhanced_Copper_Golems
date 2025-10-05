package com.philipvk.enhanced_copper_golems.events;

import com.philipvk.enhanced_copper_golems.Enhanced_copper_golems;
import com.philipvk.enhanced_copper_golems.entities.SmartCopperGolemEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class TransformCopperGolem {

    public static void initialize() {
        UseEntityCallback.EVENT.register(((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(entity instanceof CopperGolemEntity vanillaGolem)) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);

            if (!stack.isOf(Items.DIAMOND)) return ActionResult.PASS;
            SmartCopperGolemEntity smartCopperGolemEntity = Enhanced_copper_golems.SMART_COPPER_GOLEM.create(world, SpawnReason.EVENT);
            if (smartCopperGolemEntity == null) return ActionResult.PASS;

            smartCopperGolemEntity.refreshPositionAndAngles(
                    vanillaGolem.getX(), vanillaGolem.getY(), vanillaGolem.getZ(),
                    vanillaGolem.getYaw(), vanillaGolem.getPitch()
            );

            smartCopperGolemEntity.setHealth(vanillaGolem.getHealth());
            smartCopperGolemEntity.setState(vanillaGolem.getState());
            smartCopperGolemEntity.setOxidationLevel(vanillaGolem.getOxidationLevel());
            smartCopperGolemEntity.setStackInHand(Hand.MAIN_HAND, vanillaGolem.getMainHandStack());
            if (vanillaGolem.isPersistent()) {
                smartCopperGolemEntity.setPersistent();
            }

            world.spawnEntity(smartCopperGolemEntity);

            vanillaGolem.discard();

            stack.decrementUnlessCreative(1, player);

            return ActionResult.SUCCESS;
        }));
    }
}
