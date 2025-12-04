package cz.grimir.wifimanager.admin.core.aggregates

import cz.grimir.wifimanager.shared.UserId
import java.time.Instant

class User(
    val id: UserId,

    /**
     * OpenID Connect subject varue, if provided.
     */
    val oidcSubject: String?,
    /**
     * OpenID Connect authentication issuer, if provided.
     */
    val oidcIssuer: String?,

    /**
     * Primary email.
     */
    var email: String,
    /**
     * Preferred display name.
     */
    var displayName: String,
    /**
     * Profile picture URL, if provided.
     */
    var pictureUrl: String?,
    var isActive: Boolean,

    /**
     * UTC time of users first login.
     */
    val createdAt: Instant,
    /**
     * UTC time of last login.
     */
    var lastLoginAt: Instant,
)