package com.czhmc.bgt_ae2_addon.mixin;

import appeng.api.storage.ISubMenuHost;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.menu.ISubMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import appeng.core.definitions.AEBlocks;

@Mixin(WirelessAccessPointBlockEntity.class)
public abstract class WirelessAccessPointBlockEntityMixin implements ISubMenuHost {
    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        player.closeContainer();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return AEBlocks.WIRELESS_ACCESS_POINT.stack();
    }
}
