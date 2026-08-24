package com.czhmc.bgt_ae2_addon;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;

@Mod(BgtAe2Addon.MOD_ID)
public final class BgtAe2Addon {
    public static final String MOD_ID = "bgt_ae2_addon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BgtAe2Addon() {
        appeng.menu.locator.MenuLocators.register(
                WirelessAccessPointMenuLocator.class,
                WirelessAccessPointMenuLocator::writeToPacket,
                WirelessAccessPointMenuLocator::readFromPacket);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(PendingCraftCleanup.class);
    }
}
