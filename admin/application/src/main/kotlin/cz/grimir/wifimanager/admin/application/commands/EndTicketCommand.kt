package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.TicketId
import cz.grimir.wifimanager.shared.UserId

data class EndTicketCommand(
    val ticketId: TicketId,
    val userId: UserId,
)

