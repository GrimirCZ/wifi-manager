package cz.grimir.wifimanager.admin.application.queries

import cz.grimir.wifimanager.shared.core.TicketId

data class FindTicketByIdQuery(
    val id: TicketId,
)
