package com.czhmc.bgt_ae2_addon.mixin;

import com.czhmc.bgt_ae2_addon.AutoCraftingMaterialPlanner;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(BuildingUtils.class)
public abstract class BuildingUtilsMixin {
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/direwolf20/buildinggadgets2/util/BuildingUtils;removeStacksFromInventory(" +
                            "Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Z" +
                            "Lnet/minecraft/core/GlobalPos;Lnet/minecraft/core/Direction;)Z"))
    private static boolean bgtAe2Addon$allowCraftableBuildShortfall(
            Player player,
            List<ItemStack> requested,
            boolean simulate,
            GlobalPos boundPos,
            Direction direction) {
        return allowCraftableShortfall(
                player, requested, simulate, boundPos, direction);
    }

    @Redirect(
            method = "exchange",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/direwolf20/buildinggadgets2/util/BuildingUtils;removeStacksFromInventory(" +
                            "Lnet/minecraft/world/entity/player/Player;Ljava/util/List;Z" +
                            "Lnet/minecraft/core/GlobalPos;Lnet/minecraft/core/Direction;)Z"))
    private static boolean bgtAe2Addon$allowCraftableExchangeShortfall(
            Player player,
            List<ItemStack> requested,
            boolean simulate,
            GlobalPos boundPos,
            Direction direction) {
        return allowCraftableShortfall(
                player, requested, simulate, boundPos, direction);
    }

    private static boolean allowCraftableShortfall(
            Player player,
            List<ItemStack> requested,
            boolean simulate,
            GlobalPos boundPos,
            Direction direction) {
        boolean available = BuildingUtils.removeStacksFromInventory(
                player, requested, simulate, boundPos, direction);
        if (available || !simulate) {
            return available;
        }
        return AutoCraftingMaterialPlanner.hasCraftableShortfall(
                player, requested, boundPos, direction);
    }
}
