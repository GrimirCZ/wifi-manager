package cz.grimir.wifimanager.shared.application

import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import java.util.UUID

data class UserIdentitySnapshot(
    val userId: UserId,
    val identityId: UUID,
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
    val roles: Set<UserRole>,
) {
    fun can(getter: (UserRole) -> Boolean): Boolean = roles.any(getter)
}
