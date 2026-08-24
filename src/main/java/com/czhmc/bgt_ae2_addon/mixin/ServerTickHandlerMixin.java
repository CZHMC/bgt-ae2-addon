package com.czhmc.bgt_ae2_addon.mixin;

import com.czhmc.bgt_ae2_addon.AutoCraftingMaterialPlanner;
import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerTickHandler.class)
public abstract class ServerTickHandlerMixin {
    @Inject(method = "build", at = @At("HEAD"), cancellable = true)
    private static void bgtAe2Addon$beforeBuild(ServerBuildList buildList, Player player, CallbackInfo callbackInfo) {
        if (AutoCraftingMaterialPlanner.handle(buildList, player)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "exchange", at = @At("HEAD"), cancellable = true)
    private static void bgtAe2Addon$beforeExchange(ServerBuildList buildList, Player player, CallbackInfo callbackInfo) {
        if (AutoCraftingMaterialPlanner.handle(buildList, player)) {
            callbackInfo.cancel();
        }
    }
}
