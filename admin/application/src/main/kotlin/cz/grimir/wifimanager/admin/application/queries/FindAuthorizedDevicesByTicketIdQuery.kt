package cz.grimir.wifimanager.admin.application.queries

import cz.grimir.wifimanager.shared.core.TicketId

data class FindAuthorizedDevicesByTicketIdQuery(
    val ticketId: TicketId,
)
