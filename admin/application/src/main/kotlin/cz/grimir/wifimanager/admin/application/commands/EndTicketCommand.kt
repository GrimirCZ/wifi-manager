package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId

data class EndTicketCommand(
    val ticketId: TicketId,
    val userId: UserId,
)
