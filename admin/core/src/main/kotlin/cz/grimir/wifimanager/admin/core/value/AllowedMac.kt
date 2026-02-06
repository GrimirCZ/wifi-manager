package cz.grimir.wifimanager.admin.core.value

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class AllowedMac(
    val mac: String,
    val hostname: String?,
    val ownerUserId: UserId,
    val ownerDisplayName: String,
    val ownerEmail: String,
    val note: String,
    val validUntil: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
