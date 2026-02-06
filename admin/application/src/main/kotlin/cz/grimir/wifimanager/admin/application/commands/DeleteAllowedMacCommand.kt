package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot

data class DeleteAllowedMacCommand(
    val macAddress: String,
    val user: UserIdentitySnapshot,
)
