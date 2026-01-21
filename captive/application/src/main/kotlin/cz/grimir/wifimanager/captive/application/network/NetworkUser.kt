package cz.grimir.wifimanager.captive.application.network

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant
import java.util.UUID

data class NetworkUser(
    val userId: UserId,
    val identityId: UUID,
    val allowedDeviceCount: Int,
    val adminOverrideLimit: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant,
)
