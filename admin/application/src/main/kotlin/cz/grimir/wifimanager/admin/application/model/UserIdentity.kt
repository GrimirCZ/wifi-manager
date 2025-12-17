package cz.grimir.wifimanager.admin.application.model

import cz.grimir.wifimanager.shared.core.UserId
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
    var username: String,
    var firstName: String?,
    var lastName: String?,
    var pictureUrl: String?,
    val createdAt: Instant,
    var lastLoginAt: Instant,
    var roles: Set<UserRole>,
) {
    fun can(getter: (UserRole) -> Boolean): Boolean {
        return roles.any(getter)
    }
}
