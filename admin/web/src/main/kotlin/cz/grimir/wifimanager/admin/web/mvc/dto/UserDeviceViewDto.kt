package cz.grimir.wifimanager.admin.web.mvc.dto

import java.time.Instant
import java.util.UUID

data class UserDeviceViewDto(
    val userId: UUID,
    val mac: String,
    val name: String?,
    val hostname: String?,
    val isRandomized: Boolean,
    val authorizedAt: Instant,
    val lastSeenAt: Instant,
    val disconnectError: Boolean,
)
