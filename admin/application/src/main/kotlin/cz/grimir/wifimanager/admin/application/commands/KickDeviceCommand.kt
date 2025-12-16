package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId

data class KickDeviceCommand(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val userId: UserId,
)
