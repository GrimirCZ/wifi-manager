package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.core.TicketId

data class ExpireTicketCommand(
    val ticketId: TicketId,
)

