package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.shared.core.UserId

data class RemoveUserDeviceCommand(
    val userId: UserId,
    val mac: String,
)
