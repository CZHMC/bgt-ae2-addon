package com.czhmc.bgt_ae2_addon;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ISubMenuHost;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEBlockEntities;
import appeng.menu.ISubMenu;
import appeng.menu.locator.MenuHostLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record WirelessAccessPointMenuLocator(ResourceKey<Level> dimension, BlockPos pos)
        implements MenuHostLocator {

    public static void writeToPacket(WirelessAccessPointMenuLocator locator, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(locator.dimension.location());
        buffer.writeBlockPos(locator.pos);
    }

    public static WirelessAccessPointMenuLocator readFromPacket(FriendlyByteBuf buffer) {
        ResourceLocation dimensionId = buffer.readResourceLocation();
        return new WirelessAccessPointMenuLocator(
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                buffer.readBlockPos());
    }

    @Override
    public <T> @Nullable T locate(Player player, Class<T> hostInterface) {
        BlockEntity blockEntity = null;
        if (player.level().dimension().equals(dimension)) {
            blockEntity = player.level().getBlockEntity(pos);
        } else if (player.getServer() != null) {
            Level level = player.getServer().getLevel(dimension);
            if (level != null) {
                blockEntity = level.getBlockEntity(pos);
            }
        }

        if (hostInterface.isInstance(blockEntity)) {
            return hostInterface.cast(blockEntity);
        }

        if (player.level().isClientSide && hostInterface.isInstance(ClientProxy.instance())) {
            return hostInterface.cast(ClientProxy.instance());
        }

        return null;
    }

    private static final class ClientProxy extends BlockEntity implements ISubMenuHost, IActionHost {
        private static ClientProxy instance() {
            return Holder.INSTANCE;
        }

        private ClientProxy() {
            super(AEBlockEntities.WIRELESS_ACCESS_POINT.get(), BlockPos.ZERO,
                    AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState());
        }

        private ClientProxy at(BlockPos pos) {
            return this;
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            player.closeContainer();
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return AEBlocks.WIRELESS_ACCESS_POINT.stack();
        }

        @Override
        public @Nullable IGridNode getActionableNode() {
            return null;
        }
    }

    private static final class Holder {
        private static final ClientProxy INSTANCE = new ClientProxy();
    }
}
