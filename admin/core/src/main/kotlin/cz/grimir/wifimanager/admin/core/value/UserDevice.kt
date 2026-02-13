package cz.grimir.wifimanager.admin.core.value

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class UserDevice(
    val userId: UserId,
    val mac: String,
    val name: String?,
    val hostname: String?,
    val isRandomized: Boolean,
    val authorizedAt: Instant,
    val lastSeenAt: Instant,
)
