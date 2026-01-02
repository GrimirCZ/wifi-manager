package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import java.time.Instant
import java.util.UUID

/**
 * Event emitted when a user is created or a field is updated.
 */
data class UserCreatedOrUpdatedEvent(
    val userId: UserId,
    val identityId: UUID,
    val issuer: String,
    val subject: String,
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
    val roles: Set<UserRole>,
    val createdAt: Instant,
    val lastLoginAt: Instant,
)
