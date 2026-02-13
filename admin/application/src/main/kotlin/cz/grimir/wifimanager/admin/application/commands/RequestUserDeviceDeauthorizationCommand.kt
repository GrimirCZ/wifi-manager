package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId

data class RequestUserDeviceDeauthorizationCommand(
    val userId: UserId,
    val deviceMac: String,
    val requestedBy: UserIdentitySnapshot,
)
