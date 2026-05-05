package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.shared.core.UserId

data class TouchNetworkUserDeviceCommand(
    val userId: UserId,
    val mac: String,
)
