package com.czhmc.bgt_ae2_addon.mixin;

import appeng.menu.me.crafting.CraftAmountMenu;
import com.czhmc.bgt_ae2_addon.AutoCraftingMaterialPlanner;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftAmountMenu.class)
public abstract class CraftAmountMenuMixin {
    @Inject(method = "confirm", at = @At("HEAD"), cancellable = true)
    private void bgtAe2Addon$confirm(int amount, boolean craftMissingAmount, boolean autoStart,
                                     CallbackInfo callbackInfo) {
        var player = ((CraftAmountMenu) (Object) this).getPlayer();
        if (player instanceof ServerPlayer serverPlayer
                && AutoCraftingMaterialPlanner.confirmQuantitySelection(
                        serverPlayer, amount, autoStart)) {
            callbackInfo.cancel();
        }
    }
}
