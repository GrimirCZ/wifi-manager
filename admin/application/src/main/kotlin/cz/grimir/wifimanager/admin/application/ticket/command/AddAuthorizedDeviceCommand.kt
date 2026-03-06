package cz.grimir.wifimanager.admin.application.ticket.command

import cz.grimir.wifimanager.shared.core.TicketId

data class AddAuthorizedDeviceCommand(
    val ticketId: TicketId,
    val deviceMacAddress: String,
    val deviceName: String?,
)
