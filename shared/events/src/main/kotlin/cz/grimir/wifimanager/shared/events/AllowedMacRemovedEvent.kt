package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class AllowedMacRemovedEvent(
    val macAddress: String,
    val removedByUserId: UserId?,
    val removedAt: Instant,
)
