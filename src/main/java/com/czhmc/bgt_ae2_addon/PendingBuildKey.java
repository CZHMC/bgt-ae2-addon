package com.czhmc.bgt_ae2_addon;

import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import java.util.UUID;

public record PendingBuildKey(UUID buildUUID, ServerBuildList.BuildType buildType) {
}
