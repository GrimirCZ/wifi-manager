package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class NetworkUserDeviceAuthorizedEvent(
    val userId: UserId,
    val deviceMac: String,
    val deviceName: String?,
    val hostname: String?,
    val isRandomized: Boolean,
    val authorizedAt: Instant,
    val lastSeenAt: Instant,
)
