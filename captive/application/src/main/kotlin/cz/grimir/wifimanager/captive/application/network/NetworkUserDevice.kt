package cz.grimir.wifimanager.captive.application.network

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class NetworkUserDevice(
    val userId: UserId,
    val mac: String,
    val name: String?,
    val hostname: String?,
    val isRandomized: Boolean,
    val authorizedAt: Instant,
    val lastSeenAt: Instant,
)
