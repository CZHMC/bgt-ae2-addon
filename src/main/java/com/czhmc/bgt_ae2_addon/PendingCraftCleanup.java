package com.czhmc.bgt_ae2_addon;

import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.UUID;

public final class PendingCraftCleanup {
    private PendingCraftCleanup() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        List<PendingBuildKey> staleKeys = AutoCraftingMaterialPlanner.pendingKeys().stream()
                .filter(key -> isStale(key, event))
                .toList();
        for (PendingBuildKey key : staleKeys) {
            AutoCraftingMaterialPlanner.clearPending(key);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getContainer() instanceof CraftAmountMenu
                || event.getContainer() instanceof CraftConfirmMenu)) {
            AutoCraftingMaterialPlanner.onNativeMenuClose(player, event.getContainer());
        }
    }

    private static boolean isStale(PendingBuildKey key, ServerTickEvent.Pre event) {
        UUID buildUUID = key.buildUUID();
        ServerBuildList buildList = ServerTickHandler.buildMap.get(buildUUID);
        if (buildList == null || buildList.statePosList == null || buildList.statePosList.isEmpty()
                || !AutoCraftingMaterialPlanner.isSupported(buildList.buildType, buildList.needItems)) {
            return true;
        }
        ServerPlayer player = event.getServer().getPlayerList().getPlayer(buildList.playerUUID);
        if (player == null) {
            return true;
        }
        if (AutoCraftingMaterialPlanner.isAwaitingQuantitySelection(key)) {
            return !(player.containerMenu instanceof CraftAmountMenu)
                    && !(player.containerMenu instanceof CraftConfirmMenu)
                    && !AutoCraftingMaterialPlanner.hasMenuTransition(player);
        }
        return AutoCraftingMaterialPlanner.shouldClearNativePlan(key, player);
    }
}
