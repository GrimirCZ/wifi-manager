package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TicketId

data class KickDeviceCommand(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val user: UserIdentitySnapshot,
)
