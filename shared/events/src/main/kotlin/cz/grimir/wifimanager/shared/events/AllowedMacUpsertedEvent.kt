package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class AllowedMacUpsertedEvent(
    val macAddress: String,
    val ownerUserId: UserId,
    val ownerDisplayName: String,
    val ownerEmail: String,
    val validUntil: Instant?,
    val updatedAt: Instant,
)
