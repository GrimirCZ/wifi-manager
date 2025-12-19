package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId

data class CancelTicketCommand(
    val ticketId: TicketId,
    val user: UserIdentity,
)
