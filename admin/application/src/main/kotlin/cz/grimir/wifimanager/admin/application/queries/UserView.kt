package cz.grimir.wifimanager.admin.application.queries

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class UserView(
    val id: UserId,
    val email: String,
    val displayName: String,
    val pictureUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant,
)
