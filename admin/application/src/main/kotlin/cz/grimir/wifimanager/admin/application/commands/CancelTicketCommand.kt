package cz.grimir.wifimanager.admin.application.commands

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TicketId

data class CancelTicketCommand(
    val ticketId: TicketId,
    val user: UserIdentitySnapshot,
)
