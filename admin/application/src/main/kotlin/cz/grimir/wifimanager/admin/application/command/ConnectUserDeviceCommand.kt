package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class ConnectUserDeviceCommand(
    val userId: UserId,
    val mac: String,
    val connectedAt: Instant,
)
