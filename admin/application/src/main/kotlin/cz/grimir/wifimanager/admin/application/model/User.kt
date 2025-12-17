package cz.grimir.wifimanager.admin.application.model

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class User(
    val id: UserId,
    var email: String,
    var displayName: String,
    var pictureUrl: String?,
    var isActive: Boolean,
    val createdAt: Instant,
    var updatedAt: Instant,
    var lastLoginAt: Instant,
)
