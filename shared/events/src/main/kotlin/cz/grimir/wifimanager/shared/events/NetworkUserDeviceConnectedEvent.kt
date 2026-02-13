package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class NetworkUserDeviceConnectedEvent(
    val userId: UserId,
    val deviceMac: String,
    val connectedAt: Instant,
)
