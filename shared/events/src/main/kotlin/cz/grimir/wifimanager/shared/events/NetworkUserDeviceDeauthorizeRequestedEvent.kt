package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class NetworkUserDeviceDeauthorizeRequestedEvent(
    val userId: UserId,
    val deviceMac: String,
    val requestedByUserId: UserId,
    val requestedAt: Instant,
)
