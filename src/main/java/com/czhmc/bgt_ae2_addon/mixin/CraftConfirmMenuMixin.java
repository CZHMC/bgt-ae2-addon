package com.czhmc.bgt_ae2_addon.mixin;

import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.czhmc.bgt_ae2_addon.AutoCraftingMaterialPlanner;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftConfirmMenu.class)
public abstract class CraftConfirmMenuMixin {
    @Inject(method = "goBack", at = @At("HEAD"), cancellable = true)
    private void bgtAe2Addon$cancelBatch(CallbackInfo callbackInfo) {
        var menu = (CraftConfirmMenu) (Object) this;
        if (menu.getPlayer() instanceof ServerPlayer serverPlayer
                && AutoCraftingMaterialPlanner.ownsNativeMenu(serverPlayer, menu)) {
            AutoCraftingMaterialPlanner.cancelQuantitySelection(serverPlayer);
            serverPlayer.closeContainer();
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "startJob", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingService;submitJob(" +
                    "Lappeng/api/networking/crafting/ICraftingPlan;" +
                    "Lappeng/api/networking/crafting/ICraftingRequester;" +
                    "Lappeng/api/networking/crafting/ICraftingCPU;Z" +
                    "Lappeng/api/networking/security/IActionSource;" +
                    ")Lappeng/api/networking/crafting/ICraftingSubmitResult;"))
    private ICraftingSubmitResult bgtAe2Addon$observeNativeSubmit(
            appeng.api.networking.crafting.ICraftingService service,
            appeng.api.networking.crafting.ICraftingPlan plan,
            appeng.api.networking.crafting.ICraftingRequester requester,
            appeng.api.networking.crafting.ICraftingCPU cpu,
            boolean prioritizePower,
            appeng.api.networking.security.IActionSource source) {
        ICraftingSubmitResult result = service.submitJob(plan, requester, cpu, prioritizePower, source);
        var menu = (CraftConfirmMenu) (Object) this;
        if (menu.getPlayer() instanceof ServerPlayer serverPlayer
                && AutoCraftingMaterialPlanner.ownsNativeMenu(serverPlayer, menu)) {
            AutoCraftingMaterialPlanner.onNativeSubmit(serverPlayer, result);
        }
        return result;
    }
}
