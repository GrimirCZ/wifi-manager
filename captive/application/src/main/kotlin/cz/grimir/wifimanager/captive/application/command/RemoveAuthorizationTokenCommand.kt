package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.shared.TicketId

data class RemoveAuthorizationTokenCommand(
    val ticketId: TicketId,
)

