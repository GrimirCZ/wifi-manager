package cz.grimir.wifimanager.admin.application.device.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId

data class RequestUserDeviceDeauthorizationCommand(
    val userId: UserId,
    val deviceMac: String,
    val requestedBy: UserIdentitySnapshot,
)
