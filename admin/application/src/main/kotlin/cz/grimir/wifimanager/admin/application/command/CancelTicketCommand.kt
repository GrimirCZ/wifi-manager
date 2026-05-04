package cz.grimir.wifimanager.admin.application.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.TicketId

data class CancelTicketCommand(
    val ticketId: TicketId,
    val user: UserIdentitySnapshot,
)
