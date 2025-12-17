package cz.grimir.wifimanager.admin.application.commands

import java.time.Instant

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
