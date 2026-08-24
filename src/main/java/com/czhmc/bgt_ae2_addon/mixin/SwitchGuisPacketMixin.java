package com.czhmc.bgt_ae2_addon.mixin;

import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.menu.me.crafting.CraftAmountMenu;
import com.czhmc.bgt_ae2_addon.AutoCraftingMaterialPlanner;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SwitchGuisPacket.class)
public abstract class SwitchGuisPacketMixin {
    @Inject(method = "doReturnToParentMenu", at = @At("HEAD"), cancellable = true)
    private void bgtAe2Addon$skipQuantitySelection(ServerPlayer player, CallbackInfo callbackInfo) {
        if (player.containerMenu instanceof CraftAmountMenu amountMenu
                && AutoCraftingMaterialPlanner.handleQuantityMenuBack(player, amountMenu)) {
            callbackInfo.cancel();
        }
    }
}
