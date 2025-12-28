package cz.grimir.wifimanager.admin.application.queries.models

import cz.grimir.wifimanager.admin.core.aggregates.Ticket

data class TicketWithDeviceCount(
    val ticket: Ticket,
    val deviceCount: Int,
)
