package cz.grimir.wifimanager.admin.application.model

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant
import java.util.UUID

data class UserIdentity(
    val id: UUID,
    val userId: UserId,
    val issuer: String,
    val subject: String,
    var emailAtProvider: String?,
    var providerUsername: String?,
    val createdAt: Instant,
    var lastLoginAt: Instant,
)
