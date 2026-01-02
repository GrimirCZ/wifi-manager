package cz.grimir.wifimanager.shared.core

import java.time.Instant
import java.util.UUID

data class UserProfileSnapshot(
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
)

data class RoleMappingInput(
    val roles: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
)

data class ResolveUserCommand(
    val issuer: String,
    val subject: String,
    val profile: UserProfileSnapshot,
    val roleMapping: RoleMappingInput = RoleMappingInput(),
    val loginAt: Instant = Instant.now(),
)

data class ResolveUserResult(
    val userId: UUID,
    val identityId: UUID,
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
    val roles: Set<UserRole>,
)

fun interface UserDirectoryClient {
    fun resolveUser(command: ResolveUserCommand): ResolveUserResult
}
