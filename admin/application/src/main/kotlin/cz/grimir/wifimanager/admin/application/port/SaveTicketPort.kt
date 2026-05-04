package cz.grimir.wifimanager.admin.application.port

import cz.grimir.wifimanager.admin.core.aggregates.Ticket

interface SaveTicketPort {
    fun save(ticket: Ticket)
}
