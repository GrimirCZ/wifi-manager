package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.shared.core.TicketId

data class RemoveAuthorizationTokenCommand(
    val ticketId: TicketId,
)
