package cz.grimir.wifimanager.admin.application.ticket.query

import cz.grimir.wifimanager.shared.core.TicketId

data class FindAuthorizedDevicesByTicketIdQuery(
    val ticketId: TicketId,
)
