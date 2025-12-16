package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.TicketId

data class ExpireTicketCommand(
    val ticketId: TicketId,
)

