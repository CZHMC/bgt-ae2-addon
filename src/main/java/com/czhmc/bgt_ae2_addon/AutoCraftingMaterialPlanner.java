package com.czhmc.bgt_ae2_addon;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.ISubMenuHost;
import appeng.helpers.ICraftingGridMenu;
import appeng.menu.MenuOpener;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.integration.CuriosIntegration;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetUtils;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AutoCraftingMaterialPlanner {
    private static final Logger LOGGER = BgtAe2Addon.LOGGER;
    private static final Map<PendingBuildKey, PendingCraft> PENDING = new HashMap<>();
    private static final Map<UUID, PendingBuildKey> QUANTITY_SELECTIONS = new HashMap<>();
    private static final Map<UUID, PendingBuildKey> NATIVE_SELECTIONS = new HashMap<>();
    private static final Set<PendingBuildKey> CANCELLED_SELECTIONS = new HashSet<>();
    private static final Set<UUID> MENU_TRANSITIONS = new HashSet<>();

    private AutoCraftingMaterialPlanner() {
    }

    public static boolean isSupported(ServerBuildList.BuildType buildType, boolean needItems) {
        return buildType != null && PendingCraft.isSupportedBuildType(buildType.name(), needItems);
    }

    /**
     * Runs on the server thread immediately before BGT removes the next state position.
     */
    public static boolean handle(ServerBuildList buildList, Player player) {
        if (buildList == null || buildList.buildUUID == null || player == null) {
            return false;
        }
        if (!isSupported(buildList.buildType, buildList.needItems)) {
            clearForBuild(buildList.buildUUID);
            return false;
        }
        if (buildList.statePosList == null || buildList.statePosList.isEmpty() || buildList.level == null
                || !isServerThread(player.getServer())) {
            clearForBuild(buildList.buildUUID);
            return false;
        }

        if (player.isCreative()) {
            clearForBuild(buildList.buildUUID);
            return false;
        }
        if (!(buildList.level instanceof ServerLevel serverLevel)) {
            clearForBuild(buildList.buildUUID);
            return false;
        }

        PendingBuildKey pendingKey = new PendingBuildKey(buildList.buildUUID, buildList.buildType);
        PendingCraft pending = PENDING.get(pendingKey);
        if (CANCELLED_SELECTIONS.remove(pendingKey)) {
            return false;
        }

        NetworkContext network = resolveNetwork(buildList.boundPos, player);
        if (network == null) {
            clearFully(pendingKey);
            return false;
        }

        if (pending != null) {
            if (pending.submissionState() == PendingCraft.SubmissionState.FAILED) {
                if (pending.tickWait(player.getServer().getTickCount())) {
                    clearFully(pendingKey);
                    return false;
                }
                return true;
            }

            if (pending.awaitingQuantitySelection()) {
                if (pending.tickWait(player.getServer().getTickCount())) {
                    clearFully(pendingKey);
                    return false;
                }
                MENU_TRANSITIONS.remove(player.getUUID());
                if (isQuantityMenuOpen(player)) {
                    return true;
                }
                if (pending.hasQuantitySelectionRemaining()
                        && openNextQuantitySelection(pendingKey, pending, buildList, player, network)) {
                    return true;
                }
                clearFully(pendingKey);
                return false;
            }

            if (pending.isAwaitingNativePlan()) {
                if (player.containerMenu instanceof CraftConfirmMenu) {
                    MENU_TRANSITIONS.remove(player.getUUID());
                    return true;
                }
                if (MENU_TRANSITIONS.remove(player.getUUID())) {
                    if (pending.awaitingQuantitySelection()
                            && openNextQuantitySelection(
                                    pendingKey, pending, buildList, player, network)) {
                        return true;
                    }
                    return openNativePlan(pendingKey, pending, buildList, player, network);
                }
                return true;
            }

            if (!sameDimension(pending, buildList.level)) {
                clearFully(pendingKey);
                return false;
            }
            if (!pending.isSubmitted()) {
                pending.markSubmissionFailed();
                return true;
            }

            List<ItemStack> missing = findMissingMaterials(buildList, serverLevel, player, network);
            if (missing == null || missing.isEmpty()) {
                LOGGER.debug("Releasing BGT build {} after materials became available", buildList.buildUUID);
                clearFully(pendingKey);
                return false;
            }
            if (pending.tickWait(player.getServer().getTickCount())) {
                pending.markSubmissionFailed();
                return false;
            }
            return true;
        }

        List<ItemStack> missing = findMissingMaterials(buildList, serverLevel, player, network);
        if (missing == null || missing.isEmpty()) {
            return false;
        }
        return openQuantitySelection(pendingKey, missing, buildList, player);
    }

    public static boolean hasCraftableShortfall(
            Player player, List<ItemStack> requested, GlobalPos boundPos, net.minecraft.core.Direction direction) {
        if (player == null || requested == null || requested.isEmpty()) {
            return false;
        }
        NetworkContext network = resolveNetwork(boundPos, player);
        if (network == null) {
            return false;
        }
        for (ItemStack stack : requested) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (BuildingUtils.removeStacksFromInventory(
                    player, List.of(stack.copy()), true, boundPos, direction)) {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            if (key == null || !network.craftingService().isCraftable(key)) {
                return false;
            }
        }
        return true;
    }

    static int pendingCount() {
        return PENDING.size();
    }

    static List<PendingBuildKey> pendingKeys() {
        return List.copyOf(PENDING.keySet());
    }

    static void clearPending(PendingBuildKey key) {
        clearFully(key);
    }

    public static boolean confirmQuantitySelection(
            ServerPlayer player, int amount, boolean autoStart) {
        if (player == null || !(player.containerMenu instanceof CraftAmountMenu amountMenu)
                || !(amountMenu.getHost() instanceof ISubMenuHost)) {
            return false;
        }
        PendingBuildKey key = QUANTITY_SELECTIONS.get(player.getUUID());
        if (key == null) {
            return false;
        }
        PendingCraft pending = PENDING.get(key);
        ServerBuildList buildList = ServerTickHandler.buildMap.get(key.buildUUID());
        if (pending == null || buildList == null || !pending.awaitingQuantitySelection()
                || buildList.statePosList == null || buildList.statePosList.isEmpty()) {
            return false;
        }
        NetworkContext menuNetwork = resolveNetwork(buildList.boundPos, player);
        var displayed = amountMenu.getWhatToCraft();
        if (menuNetwork == null || amountMenu.getHost() != menuNetwork.accessPointEntity()
                || displayed == null || pending.selectedKey() == null
                || !pending.selectedKey().equals(displayed.what())) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        LOGGER.debug("Received AE2 quantity confirmation for BGT build {}: {} items of {}",
                key.buildUUID(), amount, pending.selectedKey());
        if (amount < pending.selectedQuantity()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.bgt_ae2_addon.crafting_quantity_too_low", pending.selectedQuantity()));
            return true;
        }
        if (!pending.acceptSelectedQuantity(amount)) {
            return true;
        }
        if (!pending.hasSelectedQuantity()) {
            return true;
        }

        QUANTITY_SELECTIONS.remove(player.getUUID());
        NetworkContext network = resolveNetwork(buildList.boundPos, player);
        if (network == null || !pending.beginNativeConfirmationAfterQuantity()) {
            pending.markNativePlanFailed();
            return true;
        }
        return openNativePlan(key, pending, buildList, player, network);
    }

    public static boolean onNativeSubmit(ServerPlayer player, ICraftingSubmitResult result) {
        if (player == null || result == null) {
            return false;
        }
        PendingBuildKey key = NATIVE_SELECTIONS.get(player.getUUID());
        if (key == null) {
            return false;
        }
        if (!result.successful()) {
            LOGGER.debug("AE2 native plan submission failed for BGT build {}: {}",
                    key.buildUUID(), result.errorCode());
            return false;
        }
        PendingCraft pending = PENDING.get(key);
        ServerBuildList buildList = ServerTickHandler.buildMap.get(key.buildUUID());
        if (pending == null || buildList == null || !pending.isAwaitingNativePlan()) {
            return false;
        }
        if (!pending.markNativeJobSubmitted()) {
            return false;
        }
        if (pending.submissionState() != PendingCraft.SubmissionState.SUBMITTED) {
            MENU_TRANSITIONS.add(player.getUUID());
        } else {
            NATIVE_SELECTIONS.remove(player.getUUID());
            LOGGER.debug("Submitted native AE2 batch for BGT build {}", key.buildUUID());
        }
        return true;
    }

    public static void onNativeMenuClose(ServerPlayer player, Object container) {
        if (player == null) {
            return;
        }
        if (MENU_TRANSITIONS.contains(player.getUUID())) {
            return;
        }
        if (container instanceof CraftAmountMenu amountMenu) {
            handleQuantityMenuBack(player, amountMenu);
            return;
        }
        cancelQuantitySelection(player);
    }

    public static boolean handleQuantityMenuBack(ServerPlayer player, CraftAmountMenu amountMenu) {
        if (player == null || amountMenu == null) {
            return false;
        }
        PendingBuildKey key = QUANTITY_SELECTIONS.get(player.getUUID());
        PendingCraft pending = key == null ? null : PENDING.get(key);
        if (key == null || pending == null
                || !PendingCraft.shouldSkipMaterialOnCancel(pending.submissionState())
                || !pending.skipSelectedQuantity()) {
            return false;
        }

        if (pending.hasQuantitySelectionRemaining()) {
            ServerBuildList buildList = ServerTickHandler.buildMap.get(key.buildUUID());
            NetworkContext network = buildList == null ? null : resolveNetwork(buildList.boundPos, player);
            ItemStack next = pending.selectedItem();
            AEItemKey nextKey = next.isEmpty() ? null : AEItemKey.of(next);
            if (buildList != null && network != null && nextKey != null) {
                MENU_TRANSITIONS.add(player.getUUID());
                CraftAmountMenu.open(player, new WirelessAccessPointMenuLocator(
                                network.accessPointEntity().getLevel().dimension(),
                                network.accessPointEntity().getBlockPos()),
                        nextKey, next.getCount());
                if (player.containerMenu instanceof CraftAmountMenu) {
                    return true;
                }
            }
        }

        clearFully(key);
        CANCELLED_SELECTIONS.add(key);
        return false;
    }

    public static void cancelQuantitySelection(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (MENU_TRANSITIONS.remove(player.getUUID())) {
            return;
        }
        PendingBuildKey key = QUANTITY_SELECTIONS.remove(player.getUUID());
        if (key == null) {
            key = NATIVE_SELECTIONS.remove(player.getUUID());
        }
        if (key != null) {
            clearFully(key);
            CANCELLED_SELECTIONS.add(key);
        }
    }

    /**
     * Handles the native plan's back/cancel action. AE2 owns the remaining native queue,
     * so only the final item closes the addon batch; earlier items let AE2 open the next plan.
     */
    public static boolean skipNativeMaterial(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        PendingBuildKey key = NATIVE_SELECTIONS.get(player.getUUID());
        PendingCraft pending = key == null ? null : PENDING.get(key);
        if (pending == null || !pending.isAwaitingNativePlan()) {
            return false;
        }
        if (!pending.skipNativeMaterial()) {
            return false;
        }
        if (pending.hasNativeMaterialRemaining()) {
            MENU_TRANSITIONS.add(player.getUUID());
            return false;
        }

        if (pending.awaitingQuantitySelection()
                && pending.hasQuantitySelectionRemaining()) {
            NATIVE_SELECTIONS.remove(player.getUUID());
            MENU_TRANSITIONS.add(player.getUUID());
            return false;
        }

        clearFully(key);
        CANCELLED_SELECTIONS.add(key);
        player.closeContainer();
        return true;
    }

    public static boolean ownsNativeMenu(ServerPlayer player, CraftConfirmMenu menu) {
        if (player == null || menu == null) {
            return false;
        }
        PendingBuildKey key = NATIVE_SELECTIONS.get(player.getUUID());
        return key != null && PENDING.containsKey(key)
                && menu.getLocator() instanceof WirelessAccessPointMenuLocator;
    }

    static boolean isQuantityMenuOpen(Player player) {
        return player != null && player.containerMenu instanceof CraftAmountMenu;
    }

    static boolean isAwaitingQuantitySelection(PendingBuildKey key) {
        PendingCraft pending = PENDING.get(key);
        return pending != null && pending.awaitingQuantitySelection();
    }

    static boolean shouldClearNativePlan(PendingBuildKey key, Player player) {
        PendingCraft pending = PENDING.get(key);
        return pending != null && PendingCraft.shouldClearNativePlan(
                pending.submissionState(),
                player != null && player.containerMenu instanceof CraftConfirmMenu,
                player != null && MENU_TRANSITIONS.contains(player.getUUID()));
    }

    static boolean hasMenuTransition(Player player) {
        return player != null && MENU_TRANSITIONS.contains(player.getUUID());
    }

    static void clearAll() {
        PENDING.clear();
        QUANTITY_SELECTIONS.clear();
        NATIVE_SELECTIONS.clear();
        CANCELLED_SELECTIONS.clear();
        MENU_TRANSITIONS.clear();
    }

    private static boolean openNextQuantitySelection(
            PendingBuildKey key,
            PendingCraft pending,
            ServerBuildList buildList,
            Player player,
            NetworkContext network) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ItemStack next = pending.selectedItem();
        AEItemKey nextKey = next.isEmpty() ? null : AEItemKey.of(next);
        if (nextKey == null) {
            return false;
        }
        MENU_TRANSITIONS.add(serverPlayer.getUUID());
        QUANTITY_SELECTIONS.put(serverPlayer.getUUID(), key);
        CraftAmountMenu.open(serverPlayer, new WirelessAccessPointMenuLocator(
                        network.accessPointEntity().getLevel().dimension(),
                        network.accessPointEntity().getBlockPos()),
                nextKey, next.getCount());
        return isQuantityMenuOpen(serverPlayer);
    }

    private static boolean openNativePlan(PendingBuildKey key, PendingCraft pending,
                                           ServerBuildList buildList, Player player, NetworkContext network) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !pending.isAwaitingNativePlan()) {
            pending.markNativePlanFailed();
            return true;
        }

        ItemStack selected = pending.nativeMaterial();
        AEItemKey selectedKey = selected.isEmpty() ? null : AEItemKey.of(selected);
        if (selectedKey == null) {
            pending.markNativePlanFailed();
            return true;
        }
        List<ICraftingGridMenu.AutoCraftEntry> entries = List.of(
                new ICraftingGridMenu.AutoCraftEntry(
                        selectedKey,
                        virtualSlots(selected.getCount())));
        if (entries.isEmpty()) {
            pending.markNativePlanFailed();
            return true;
        }

        NATIVE_SELECTIONS.put(serverPlayer.getUUID(), key);
        MENU_TRANSITIONS.add(serverPlayer.getUUID());
        WirelessAccessPointMenuLocator locator = new WirelessAccessPointMenuLocator(
                network.accessPointEntity().getLevel().dimension(),
                network.accessPointEntity().getBlockPos());
        CraftConfirmMenu.openWithCraftingList(
                network.accessPoint(), serverPlayer, locator, entries);
        boolean opened = serverPlayer.containerMenu instanceof CraftConfirmMenu;
        LOGGER.debug("Opening AE2 native batch plan for BGT build {}: opened={}, entries={}",
                key.buildUUID(), opened, entries.size());
        if (!opened) {
            pending.markNativePlanFailed();
        }
        return true;
    }

    private static List<Integer> virtualSlots(int size) {
        return new AbstractList<>() {
            @Override
            public Integer get(int index) {
                if (index < 0 || index >= size) {
                    throw new IndexOutOfBoundsException(index);
                }
                return index;
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    private static boolean openQuantitySelection(PendingBuildKey key, List<ItemStack> missing,
                                                  ServerBuildList buildList, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        NetworkContext network = resolveNetwork(buildList.boundPos, player);
        if (network == null || missing.isEmpty()) {
            return false;
        }
        PendingCraft pending = PendingCraft.forQuantitySelection(key, missing, buildList.level.dimension());
        ItemStack selected = pending.selectedItem();
        AEItemKey selectedKey = selected.isEmpty() ? null : AEItemKey.of(selected);
        if (selectedKey == null || selected.getCount() <= 0) {
            return false;
        }
        PENDING.put(key, pending);
        QUANTITY_SELECTIONS.put(serverPlayer.getUUID(), key);
        CraftAmountMenu.open(serverPlayer, new WirelessAccessPointMenuLocator(
                        network.accessPointEntity().getLevel().dimension(),
                        network.accessPointEntity().getBlockPos()),
                selectedKey, selected.getCount());
        if (!isQuantityMenuOpen(serverPlayer)) {
            clearFully(key);
            return false;
        }
        return true;
    }

    private static List<ItemStack> findMissingMaterials(ServerBuildList buildList, ServerLevel level,
                                                        Player player, NetworkContext network) {
        MaterialReservation reservation = MaterialReservation.create(buildList, player, network);
        Map<AEItemKey, ItemStack> missing = new LinkedHashMap<>();

        for (StatePos statePos : new ArrayList<>(buildList.statePosList)) {
            if (!isMaterialEntry(buildList, statePos, level, player)) {
                continue;
            }

            BlockPos target = targetPosition(buildList, statePos);
            List<ItemStack> drops;
            try {
                drops = GadgetUtils.getDropsForBlockState(level, target, statePos.state, player);
            } catch (RuntimeException exception) {
                LOGGER.debug("Could not calculate BGT drops for {}", target, exception);
                continue;
            }

            for (ItemStack drop : drops) {
                if (drop.isEmpty() || reservation.reserve(drop)) {
                    continue;
                }
                AEItemKey key = AEItemKey.of(drop);
                if (key == null || !PendingCraft.shouldRequestMissingMaterial(network.craftingService().isCraftable(key))) {
                    continue;
                }
                ItemStack existing = missing.get(key);
                if (existing == null) {
                    missing.put(key, drop.copy());
                } else {
                    existing.grow(drop.getCount());
                }
            }
        }

        return new ArrayList<>(missing.values());
    }

    private static boolean isMaterialEntry(ServerBuildList buildList, StatePos statePos,
                                           ServerLevel level, Player player) {
        if (statePos == null || statePos.state == null || statePos.pos == null
                || statePos.state.isAir() || !statePos.state.getFluidState().isEmpty()
                || player.isCreative()) {
            return false;
        }

        BlockPos target = targetPosition(buildList, statePos);
        if (buildList.retryList.contains(target)) {
            return false;
        }
        if (buildList.buildType == ServerBuildList.BuildType.EXCHANGE) {
            return !level.getBlockState(target).equals(statePos.state)
                    && statePos.state.canSurvive(level, target);
        }
        return statePos.state.canSurvive(level, target)
                && level.getBlockState(target).canBeReplaced();
    }

    private static final class MaterialReservation {
        private final Map<AEItemKey, Long> network;
        private final List<ItemStack> boundStacks;
        private final List<ItemStack> curiousAndInventoryStacks;
        private final Player player;
        private final Map<AEItemKey, Long> curiousReserved = new HashMap<>();

        private MaterialReservation(Map<AEItemKey, Long> network, List<ItemStack> boundStacks,
                                    List<ItemStack> curiousAndInventoryStacks, Player player) {
            this.network = network;
            this.boundStacks = boundStacks;
            this.curiousAndInventoryStacks = curiousAndInventoryStacks;
            this.player = player;
        }

        static MaterialReservation create(ServerBuildList buildList, Player player, NetworkContext network) {
            Map<AEItemKey, Long> networkStacks = new HashMap<>();
            IGrid grid = network.accessPoint().getGrid();
            if (grid != null) {
                for (var entry : grid.getStorageService().getInventory().getAvailableStacks()) {
                    if (entry.getKey() instanceof AEItemKey itemKey && entry.getLongValue() > 0) {
                        networkStacks.put(itemKey, entry.getLongValue());
                    }
                }
            }

            List<ItemStack> boundStacks = new ArrayList<>();
            IItemHandler boundHandler = BuildingUtils.getHandlerFromBound(
                    player, buildList.boundPos, buildList.getDirection());
            addHandlerStacks(boundHandler, boundStacks);

            List<ItemStack> curiousStacks = new ArrayList<>();
            addCuriosStacks(player, curiousStacks);
            List<ItemStack> inventoryStacks = new ArrayList<>();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                addStackOrHandler(player.getInventory().getItem(slot), inventoryStacks);
            }
            curiousStacks.addAll(inventoryStacks);
            return new MaterialReservation(networkStacks, boundStacks, curiousStacks, player);
        }

        boolean reserve(ItemStack requested) {
            AEItemKey key = AEItemKey.of(requested);
            if (key == null) {
                return false;
            }
            long amount = requested.getCount();
            long networkAmount = network.getOrDefault(key, 0L);
            if (networkAmount >= amount) {
                network.put(key, networkAmount - amount);
                return true;
            }
            if (reserveFromStacks(boundStacks, requested)) {
                return true;
            }
            return reserveFromStacks(curiousAndInventoryStacks, requested);
        }

        private static void addCuriosStacks(Player player, List<ItemStack> target) {
            if (!CuriosIntegration.isLoaded()) {
                return;
            }
            try {
                Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Object result = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
                        .invoke(null, player);
                if (!(result instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                    return;
                }
                Object curios = optional.get();
                Object handlers = curios.getClass().getMethod("getCurios").invoke(curios);
                if (handlers instanceof Map<?, ?> map) {
                    for (Object handler : map.values()) {
                        Object stacks = handler.getClass().getMethod("getStacks").invoke(handler);
                        if (stacks instanceof IItemHandler itemHandler) {
                            addHandlerStacks(itemHandler, target);
                        }
                    }
                }
            } catch (ReflectiveOperationException exception) {
                LOGGER.debug("Could not inspect Curios inventory for batch reservation", exception);
            }
        }

        private static boolean reserveFromStacks(List<ItemStack> stacks, ItemStack requested) {
            for (ItemStack stack : stacks) {
                if (ItemStack.isSameItem(stack, requested) && stack.getCount() >= requested.getCount()) {
                    stack.shrink(requested.getCount());
                    return true;
                }
            }
            return false;
        }

        private static void addHandlerStacks(IItemHandler handler, List<ItemStack> target) {
            if (handler == null) {
                return;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                addStackOrHandler(handler.getStackInSlot(slot), target);
            }
        }

        private static void addStackOrHandler(ItemStack stack, List<ItemStack> target) {
            if (stack.isEmpty()) {
                return;
            }
            IItemHandler nested = stack.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM, null);
            if (nested != null) {
                addHandlerStacks(nested, target);
            } else {
                target.add(stack.copy());
            }
        }
    }

    private static NetworkContext resolveNetwork(GlobalPos boundPos, Player player) {
        if (boundPos == null || player.getServer() == null) {
            return null;
        }
        Level level = BuildingUtils.getLevel(player.getServer(), boundPos);
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(boundPos.pos());
        if (!(blockEntity instanceof IWirelessAccessPoint accessPoint) || !accessPoint.isActive()) {
            return null;
        }
        IGrid grid = accessPoint.getGrid();
        IGridNode node = accessPoint.getActionableNode();
        if (grid == null || node == null || !node.isActive()) {
            return null;
        }
        ICraftingService craftingService = grid.getCraftingService();
        if (craftingService == null) {
            return null;
        }
        return new NetworkContext(accessPoint, blockEntity, node, craftingService,
                IActionSource.ofPlayer(player, accessPoint));
    }

    private static BlockPos targetPosition(ServerBuildList buildList, StatePos statePos) {
        if (buildList.buildType == ServerBuildList.BuildType.BUILD) {
            return statePos.pos.offset(buildList.lookingAt == null ? BlockPos.ZERO : buildList.lookingAt);
        }
        if (buildList.lookingAt != null
                && !buildList.lookingAt.equals(com.direwolf20.buildinggadgets2.util.GadgetNBT.nullPos)) {
            return statePos.pos.offset(buildList.lookingAt);
        }
        return statePos.pos;
    }

    private static boolean sameDimension(PendingCraft pending, Level level) {
        return pending.dimension() == null || pending.dimension().equals(level.dimension());
    }

    private static boolean isServerThread(MinecraftServer server) {
        return server != null && server.isSameThread();
    }

    private static void clearFully(PendingBuildKey key) {
        if (key == null) {
            return;
        }
        PENDING.remove(key);
        QUANTITY_SELECTIONS.entrySet().removeIf(entry -> {
            if (key.equals(entry.getValue())) {
                MENU_TRANSITIONS.remove(entry.getKey());
                return true;
            }
            return false;
        });
        NATIVE_SELECTIONS.entrySet().removeIf(entry -> {
            if (key.equals(entry.getValue())) {
                MENU_TRANSITIONS.remove(entry.getKey());
                return true;
            }
            return false;
        });
        CANCELLED_SELECTIONS.remove(key);
    }

    private static void clearForBuild(UUID buildUUID) {
        if (buildUUID == null) {
            return;
        }
        for (PendingBuildKey staleKey : allStateKeys().stream()
                .filter(key -> buildUUID.equals(key.buildUUID()))
                .toList()) {
            clearFully(staleKey);
        }
    }

    private static Set<PendingBuildKey> allStateKeys() {
        return new HashSet<>(PENDING.keySet());
    }

    private record NetworkContext(IWirelessAccessPoint accessPoint, BlockEntity accessPointEntity,
                                  IGridNode node, ICraftingService craftingService, IActionSource actionSource) {
    }
}
