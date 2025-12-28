package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.shared.core.TicketId

data class KickDeviceCommand(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val user: UserIdentity,
)
