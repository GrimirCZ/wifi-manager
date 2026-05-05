package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.shared.core.UserId

data class RemoveNetworkUserDeviceCommand(
    val userId: UserId,
    val mac: String,
)
