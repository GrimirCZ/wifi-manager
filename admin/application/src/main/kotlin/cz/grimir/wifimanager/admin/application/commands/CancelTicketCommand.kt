package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.TicketId
import cz.grimir.wifimanager.shared.UserId
import java.time.Duration

data class CancelTicketCommand(
    val ticketId: TicketId,
    val userId: UserId
)
