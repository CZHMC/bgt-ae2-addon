package com.czhmc.bgt_ae2_addon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PendingCraftTest {
    @Test
    void onlyBuildAndExchangeQueuesAreSupported() {
        assertTrue(PendingCraft.isSupportedBuildType("BUILD", true));
        assertTrue(PendingCraft.isSupportedBuildType("EXCHANGE", true));
        assertFalse(PendingCraft.isSupportedBuildType("CUT", true));
        assertFalse(PendingCraft.isSupportedBuildType("DESTROY", true));
        assertFalse(PendingCraft.isSupportedBuildType("UNDO_DESTROY", true));
        assertFalse(PendingCraft.isSupportedBuildType("BUILD", false));
    }

    @Test
    void missingMaterialIsRequestableOnlyWithACraftingPattern() {
        assertTrue(PendingCraft.shouldRequestMissingMaterial(true));
        assertFalse(PendingCraft.shouldRequestMissingMaterial(false));
    }

    @Test
    void quantityConfirmationAlwaysOpensNativePlanBeforeNextQuantity() {
        assertTrue(PendingCraft.shouldOpenNativePlanAfterQuantity(true));
        assertFalse(PendingCraft.shouldOpenNativePlanAfterQuantity(false));
    }

    @Test
    void submittedNativeMaterialReturnsToQuantitySelectionWhenMoreIsUnselected() {
        assertEquals(
                PendingCraft.SubmissionState.AWAITING_CONFIRMATION,
                PendingCraft.stateAfterNativeSubmission(true, false));
        assertEquals(
                PendingCraft.SubmissionState.NATIVE_PLANNING,
                PendingCraft.stateAfterNativeSubmission(false, true));
        assertEquals(
                PendingCraft.SubmissionState.SUBMITTED,
                PendingCraft.stateAfterNativeSubmission(false, false));
    }

    @Test
    void craftableMissingMaterialMayRemainInTheInitialBgtQueue() {
        assertTrue(PendingCraft.shouldAllowInitialMaterialReservation(true, false));
        assertTrue(PendingCraft.shouldAllowInitialMaterialReservation(false, true));
        assertFalse(PendingCraft.shouldAllowInitialMaterialReservation(false, false));
    }

    @Test
    void nativePlanSkipCanReturnToQuantitySelection() {
        assertEquals(
                PendingCraft.SubmissionState.AWAITING_CONFIRMATION,
                PendingCraft.stateAfterNativeSkip(true, false));
        assertEquals(
                PendingCraft.SubmissionState.NATIVE_PLANNING,
                PendingCraft.stateAfterNativeSkip(false, true));
        assertEquals(
                PendingCraft.SubmissionState.SUBMITTED,
                PendingCraft.stateAfterNativeSkip(false, false));
    }

    @Test
    void nativePlanCancellationMustBeInterceptedBeforeNextQuantitySelection() {
        assertTrue(
                PendingCraft.shouldInterceptNativePlanCancel(
                        true,
                        false));
        assertFalse(
                PendingCraft.shouldInterceptNativePlanCancel(
                        false,
                        false));
    }

    @Test
    void finalNativePlanCancellationMustResumeTheBgtQueue() {
        assertTrue(
                PendingCraft.shouldResumeBgtAfterCancellation(
                        false,
                        false));
        assertFalse(
                PendingCraft.shouldResumeBgtAfterCancellation(
                        true,
                        false));
        assertFalse(
                PendingCraft.shouldResumeBgtAfterCancellation(
                        false,
                        true));
    }

    @Test
    void cancelledMaterialIsSkippedOnlyForTheSameBuild() {
        var firstBuild = java.util.UUID.randomUUID();
        var secondBuild = java.util.UUID.randomUUID();

        assertTrue(PendingCraft.shouldSkipCancelledMaterial(
                new PendingBuildKey(firstBuild,
                        com.direwolf20.buildinggadgets2.common.events.ServerBuildList.BuildType.BUILD),
                new PendingBuildKey(firstBuild,
                        com.direwolf20.buildinggadgets2.common.events.ServerBuildList.BuildType.BUILD)));
        assertFalse(PendingCraft.shouldSkipCancelledMaterial(
                new PendingBuildKey(secondBuild,
                        com.direwolf20.buildinggadgets2.common.events.ServerBuildList.BuildType.BUILD),
                new PendingBuildKey(firstBuild,
                        com.direwolf20.buildinggadgets2.common.events.ServerBuildList.BuildType.BUILD)));
    }

    @Test
    void pendingBuildIdentityIsStableWhileBgtAdvancesItsQueue() {
        var buildUuid = java.util.UUID.randomUUID();

        assertEquals(
                new PendingBuildKey(buildUuid, com.direwolf20.buildinggadgets2.common.events.ServerBuildList.BuildType.BUILD),
                new PendingBuildKey(buildUuid, com.direwolf20.buildinggadgets2.common.events.ServerBuildList.BuildType.BUILD));
    }

    @Test
    void nativePlanConfirmationRequiresSelectedMaterials() {
        PendingCraft pending = PendingCraft.empty(null);

        assertFalse(pending.beginNativeConfirmation());
        assertFalse(pending.isAwaitingNativePlan());
    }

    @Test
    void skippingNativeMaterialOnlyContinuesWhenAnotherMaterialRemains() {
        assertTrue(PendingCraft.hasNativeMaterialAfterSkip(0, 2));
        assertFalse(PendingCraft.hasNativeMaterialAfterSkip(1, 2));
        assertFalse(PendingCraft.hasNativeMaterialAfterSkip(0, 1));
    }

    @Test
    void onlyQuantitySelectionCancellationSkipsItsCurrentMaterial() {
        assertTrue(PendingCraft.shouldSkipMaterialOnCancel(
                PendingCraft.SubmissionState.AWAITING_CONFIRMATION));
        assertFalse(PendingCraft.shouldSkipMaterialOnCancel(
                PendingCraft.SubmissionState.NATIVE_PLANNING));
        assertFalse(PendingCraft.shouldSkipMaterialOnCancel(
                PendingCraft.SubmissionState.SUBMITTED));
    }

    @Test
    void nativePlanIsOnlyStaleAfterItsMenuTransitionHasFinished() {
        assertTrue(PendingCraft.shouldClearNativePlan(
                PendingCraft.SubmissionState.NATIVE_PLANNING, false, false));
        assertFalse(PendingCraft.shouldClearNativePlan(
                PendingCraft.SubmissionState.NATIVE_PLANNING, false, true));
        assertFalse(PendingCraft.shouldClearNativePlan(
                PendingCraft.SubmissionState.NATIVE_PLANNING, true, false));
        assertFalse(PendingCraft.shouldClearNativePlan(
                PendingCraft.SubmissionState.FAILED, false, false));
    }

    @Test
    void failedNativePlanCanBeRetriedWithoutReturningToQuantitySelection() {
        PendingCraft pending = PendingCraft.empty(null);

        pending.markNativePlanFailed();

        assertEquals(PendingCraft.SubmissionState.FAILED, pending.submissionState());
        assertFalse(pending.awaitingQuantitySelection());
    }

    @Test
    void repeatedServerTickDoesNotAdvanceTimeoutTwice() {
        PendingCraft pending = PendingCraft.empty(null);

        assertFalse(pending.tickWait(42));
        assertFalse(pending.tickWait(42));
        for (int tick = 43; tick < 42 + PendingCraft.MAX_WAIT_TICKS; tick++) {
            pending.tickWait(tick);
        }
        assertTrue(pending.tickWait(42 + PendingCraft.MAX_WAIT_TICKS));
    }

    @Test
    void failedCraftHasBoundedWaitAndCanReturnControlToBgt() {
        PendingCraft pending = PendingCraft.empty(null);

        pending.markSubmissionFailed();
        for (int i = 0; i < PendingCraft.MAX_WAIT_TICKS - 1; i++) {
            assertFalse(pending.tickWait());
        }
        assertTrue(pending.tickWait());
        assertTrue(PendingCraft.failureReturnsToBgt());
    }
}
