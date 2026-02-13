package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class NetworkUserDeviceRemovedEvent(
    val userId: UserId,
    val deviceMac: String,
    val removedAt: Instant,
)
