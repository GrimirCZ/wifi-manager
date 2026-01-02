package cz.grimir.wifimanager.user.application.ports

import cz.grimir.wifimanager.shared.core.UserRole
import java.time.Instant
import java.util.UUID

data class UserIdentityRecord(
    val identityId: UUID,
    val userId: UUID,
    val issuer: String,
    val subject: String,
    val displayName: String,
    val email: String,
    val pictureUrl: String?,
    val roles: Set<UserRole>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant,
    val inserted: Boolean,
)

interface UserDirectoryRepository {
    fun findByIssuerAndSubject(
        issuer: String,
        subject: String,
    ): UserIdentityRecord?

    fun createIdentity(
        identityId: UUID,
        issuer: String,
        subject: String,
        displayName: String,
        email: String,
        pictureUrl: String?,
        roles: Set<UserRole>,
        loginAt: Instant,
    ): UserIdentityRecord?

    fun updateIdentity(
        identityId: UUID,
        displayName: String,
        email: String,
        pictureUrl: String?,
        roles: Set<UserRole>,
        loginAt: Instant,
    ): UserIdentityRecord

    fun updateLastLoginAt(
        identityId: UUID,
        loginAt: Instant,
    )
}
