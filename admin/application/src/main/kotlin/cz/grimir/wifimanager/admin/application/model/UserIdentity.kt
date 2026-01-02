package cz.grimir.wifimanager.admin.application.model

import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import java.time.Instant
import java.util.UUID

data class UserIdentity(
    /**
     * Unique identifier of a single user identity..
     */
    val id: UUID,
    /**
     * Unique identifier of the user in the system.
     */
    val userId: UserId,
    val issuer: String,
    val subject: String,
    var email: String,
    var displayName: String,
    var pictureUrl: String?,
    val createdAt: Instant,
    var roles: Set<UserRole>,
) {
    fun can(getter: (UserRole) -> Boolean): Boolean = roles.any(getter)
}
