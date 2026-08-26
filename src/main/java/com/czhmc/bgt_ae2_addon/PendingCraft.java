package com.czhmc.bgt_ae2_addon;

import appeng.api.stacks.AEItemKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PendingCraft {
    public static final int MAX_WAIT_TICKS = 1200;

    public enum SubmissionState {
        AWAITING_CONFIRMATION,
        NATIVE_PLANNING,
        CALCULATING,
        SUBMITTED,
        FAILED
    }

    private final PendingBuildKey key;
    private final ResourceKey<Level> dimension;
    private final List<ItemStack> requiredItems;
    private final List<ItemStack> selectedItems = new ArrayList<>();
    private int quantitySelectionIndex;
    private int nativeMaterialIndex;
    private int ageTicks;
    private int lastTick = Integer.MIN_VALUE;
    private SubmissionState submissionState = SubmissionState.CALCULATING;

    private PendingCraft(PendingBuildKey key, List<ItemStack> requiredItems, ResourceKey<Level> dimension) {
        this.key = key;
        this.dimension = dimension;
        this.requiredItems = copyAndAggregate(requiredItems);
    }

    public static boolean isSupportedBuildType(String buildType, boolean needItems) {
        return needItems && ("BUILD".equals(buildType) || "EXCHANGE".equals(buildType));
    }

    public static boolean shouldRequestMissingMaterial(boolean hasCraftingPattern) {
        return true;
    }

    public static boolean shouldOpenNativePlanAfterQuantity(boolean hasSelectedQuantity) {
        return hasSelectedQuantity;
    }

    public static SubmissionState stateAfterNativeSubmission(
            boolean hasUnselectedQuantity, boolean hasNativeMaterialRemaining) {
        if (hasUnselectedQuantity) {
            return SubmissionState.AWAITING_CONFIRMATION;
        }
        if (hasNativeMaterialRemaining) {
            return SubmissionState.NATIVE_PLANNING;
        }
        return SubmissionState.SUBMITTED;
    }

    public static SubmissionState stateAfterNativeSkip(
            boolean hasUnselectedQuantity, boolean hasNativeMaterialRemaining) {
        if (hasUnselectedQuantity && !hasNativeMaterialRemaining) {
            return SubmissionState.AWAITING_CONFIRMATION;
        }
        if (hasNativeMaterialRemaining) {
            return SubmissionState.NATIVE_PLANNING;
        }
        return SubmissionState.SUBMITTED;
    }

    public static boolean shouldInterceptNativePlanCancel(
            boolean hasUnselectedQuantity, boolean hasNativeMaterialRemaining) {
        return hasUnselectedQuantity && !hasNativeMaterialRemaining;
    }

    public static boolean shouldResumeBgtAfterCancellation(
            boolean hasUnselectedQuantity, boolean hasNativeMaterialRemaining) {
        return !hasUnselectedQuantity && !hasNativeMaterialRemaining;
    }

    public static boolean shouldSkipCancelledMaterial(
            PendingBuildKey buildKey,
            PendingBuildKey cancelledBuildKey) {
        return buildKey != null && buildKey.equals(cancelledBuildKey);
    }

    public static boolean shouldAllowInitialMaterialReservation(
            boolean hasAvailableMaterial, boolean hasCraftingPattern) {
        return true;
    }

    public static boolean failureReturnsToBgt() {
        return true;
    }

    public static boolean hasNativeMaterialAfterSkip(int materialIndex, int materialCount) {
        return materialIndex >= 0 && materialIndex + 1 < materialCount;
    }

    public static boolean shouldSkipMaterialOnCancel(SubmissionState state) {
        return state == SubmissionState.AWAITING_CONFIRMATION;
    }

    public static boolean shouldClearNativePlan(
            SubmissionState state, boolean menuOpen, boolean transitionPending) {
        return state == SubmissionState.NATIVE_PLANNING && !menuOpen && !transitionPending;
    }

    public static PendingCraft empty(PendingBuildKey key) {
        return new PendingCraft(key, List.of(), null);
    }

    public static PendingCraft create(PendingBuildKey key, List<ItemStack> requiredItems,
                                      ResourceKey<Level> dimension) {
        return new PendingCraft(key, requiredItems, dimension);
    }

    public static PendingCraft forQuantitySelection(PendingBuildKey key, List<ItemStack> requiredItems,
                                                     ResourceKey<Level> dimension) {
        PendingCraft pending = new PendingCraft(key, requiredItems, dimension);
        pending.submissionState = SubmissionState.AWAITING_CONFIRMATION;
        return pending;
    }

    public PendingBuildKey key() {
        return key;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public List<ItemStack> requiredItems() {
        return requiredItems.stream().map(ItemStack::copy).toList();
    }

    public List<ItemStack> missingItems() {
        return requiredItems();
    }

    public boolean awaitingQuantitySelection() {
        return submissionState == SubmissionState.AWAITING_CONFIRMATION;
    }

    public boolean beginNativeConfirmation() {
        if (selectedItems.isEmpty() || submissionState != SubmissionState.CALCULATING) {
            return false;
        }
        submissionState = SubmissionState.NATIVE_PLANNING;
        return true;
    }

    public boolean beginNativeConfirmationAfterQuantity() {
        if (selectedItems.isEmpty()
                || (submissionState != SubmissionState.AWAITING_CONFIRMATION
                        && submissionState != SubmissionState.CALCULATING)) {
            return false;
        }
        submissionState = SubmissionState.NATIVE_PLANNING;
        return true;
    }

    public boolean hasSelectedQuantity() {
        return !selectedItems.isEmpty();
    }

    public boolean hasUnselectedQuantity() {
        return quantitySelectionIndex < requiredItems.size();
    }

    public boolean resumeQuantitySelection() {
        if (submissionState != SubmissionState.NATIVE_PLANNING
                || !hasUnselectedQuantity()) {
            return false;
        }
        submissionState = SubmissionState.AWAITING_CONFIRMATION;
        return true;
    }

    public boolean isAwaitingNativePlan() {
        return submissionState == SubmissionState.NATIVE_PLANNING;
    }

    public ItemStack nativeMaterial() {
        if (!isAwaitingNativePlan() || nativeMaterialIndex >= selectedItems.size()) {
            return ItemStack.EMPTY;
        }
        return selectedItems.get(nativeMaterialIndex).copy();
    }

    public boolean skipNativeMaterial() {
        if (!isAwaitingNativePlan() || nativeMaterialIndex >= selectedItems.size()) {
            return false;
        }
        int skippedIndex = nativeMaterialIndex++;
        if (!hasNativeMaterialAfterSkip(skippedIndex, selectedItems.size())) {
            submissionState = stateAfterNativeSkip(
                    hasUnselectedQuantity(), false);
        }
        return true;
    }

    public boolean hasNativeMaterialRemaining() {
        return isAwaitingNativePlan() && nativeMaterialIndex < selectedItems.size();
    }

    public void markNativePlanFailed() {
        submissionState = SubmissionState.FAILED;
    }

    public boolean markNativeJobSubmitted() {
        if (submissionState != SubmissionState.NATIVE_PLANNING) {
            return false;
        }
        nativeMaterialIndex++;
        if (nativeMaterialIndex < selectedItems.size()) {
            return true;
        }
        submissionState = stateAfterNativeSubmission(
                hasUnselectedQuantity(), false);
        return true;
    }

    public int quantitySelectionIndex() {
        return quantitySelectionIndex;
    }

    public ItemStack selectedItem() {
        if (!awaitingQuantitySelection() || quantitySelectionIndex >= requiredItems.size()) {
            return ItemStack.EMPTY;
        }
        return requiredItems.get(quantitySelectionIndex).copy();
    }

    public AEItemKey selectedKey() {
        ItemStack selected = selectedItem();
        return selected.isEmpty() ? null : AEItemKey.of(selected);
    }

    public int selectedQuantity() {
        ItemStack selected = selectedItem();
        return selected.isEmpty() ? 0 : selected.getCount();
    }

    public boolean acceptSelectedQuantity(int amount) {
        if (!awaitingQuantitySelection() || amount <= 0 || quantitySelectionIndex >= requiredItems.size()) {
            return false;
        }
        ItemStack selected = requiredItems.get(quantitySelectionIndex).copy();
        selected.setCount(amount);
        selectedItems.add(selected);
        quantitySelectionIndex++;
        if (quantitySelectionIndex >= requiredItems.size()) {
            submissionState = SubmissionState.CALCULATING;
        }
        return true;
    }

    public boolean skipSelectedQuantity() {
        if (!awaitingQuantitySelection() || quantitySelectionIndex >= requiredItems.size()) {
            return false;
        }
        quantitySelectionIndex++;
        return true;
    }

    public boolean hasQuantitySelectionRemaining() {
        return awaitingQuantitySelection() && quantitySelectionIndex < requiredItems.size();
    }

    public List<ItemStack> selectedItems() {
        return selectedItems.stream().map(ItemStack::copy).toList();
    }

    public boolean isSubmitted() {
        return submissionState == SubmissionState.SUBMITTED;
    }

    public SubmissionState submissionState() {
        return submissionState;
    }

    public void markSubmissionFailed() {
        submissionState = SubmissionState.FAILED;
    }

    public boolean tickWait() {
        return tickWait(lastTick == Integer.MAX_VALUE ? Integer.MIN_VALUE : lastTick + 1);
    }

    public boolean tickWait(int serverTick) {
        if (serverTick == lastTick) {
            return ageTicks >= MAX_WAIT_TICKS;
        }
        lastTick = serverTick;
        ageTicks++;
        return ageTicks >= MAX_WAIT_TICKS;
    }

    private static List<ItemStack> copyAndAggregate(List<ItemStack> items) {
        Map<AEItemKey, ItemStack> aggregated = new LinkedHashMap<>();
        for (ItemStack item : items) {
            if (item.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(item);
            if (key == null) {
                continue;
            }
            ItemStack existing = aggregated.get(key);
            if (existing == null) {
                aggregated.put(key, item.copy());
            } else {
                existing.grow(item.getCount());
            }
        }
        return new ArrayList<>(aggregated.values());
    }

}
