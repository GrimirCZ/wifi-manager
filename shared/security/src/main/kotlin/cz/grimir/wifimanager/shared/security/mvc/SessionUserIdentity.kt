package cz.grimir.wifimanager.shared.security.mvc

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import java.io.Serializable
import java.util.UUID

data class SessionUserIdentity(
    val userId: UUID,
    val identityId: UUID,
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
    val roles: Set<UserRole>,
) : Serializable {
    fun toSnapshot(): UserIdentitySnapshot =
        UserIdentitySnapshot(
            userId = UserId(userId),
            identityId = identityId,
            displayName = displayName,
            email = email,
            pictureUrl = pictureUrl,
            roles = roles,
        )

    companion object {
        const val SESSION_KEY: String = "currentUser"
    }
}
