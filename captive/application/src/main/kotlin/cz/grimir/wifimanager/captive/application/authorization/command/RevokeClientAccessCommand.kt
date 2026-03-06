package cz.grimir.wifimanager.captive.application.authorization.command

import cz.grimir.wifimanager.shared.core.TicketId

data class RevokeClientAccessCommand(
    val ticketId: TicketId,
    val deviceMacAddress: String,
)
