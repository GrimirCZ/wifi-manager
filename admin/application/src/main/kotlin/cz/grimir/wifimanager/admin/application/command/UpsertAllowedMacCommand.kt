package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import java.time.Duration

data class UpsertAllowedMacCommand(
    val macAddress: String,
    val hostname: String?,
    val note: String,
    val validityDuration: Duration?,
    val user: UserIdentitySnapshot,
)
