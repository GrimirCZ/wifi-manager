package cz.grimir.wifimanager.admin.application.commands

import java.time.Instant

/**
 * Represents the normalized user information received from an OIDC login.
 *
 * Used to create/update the local user record and the (issuer, subject) identity mapping.
 */
data class UpsertUserFromLoginCommand(
    val issuer: String,
    val subject: String,
    val email: String,
    val displayName: String,
    val pictureUrl: String?,
    val emailAtProvider: String? = null,
    val providerUsername: String? = null,
    val loginAt: Instant = Instant.now(),
)
