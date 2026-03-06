package cz.grimir.wifimanager.admin.application.allowedmac.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot

data class DeleteAllowedMacCommand(
    val macAddress: String,
    val user: UserIdentitySnapshot,
)
