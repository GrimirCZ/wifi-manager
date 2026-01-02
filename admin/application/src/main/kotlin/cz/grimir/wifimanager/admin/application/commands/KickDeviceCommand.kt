package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TicketId

data class KickDeviceCommand(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val user: UserIdentitySnapshot,
)
