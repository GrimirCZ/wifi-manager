package cz.grimir.wifimanager.admin.application.ticket.port

import cz.grimir.wifimanager.admin.core.aggregates.Ticket

interface SaveTicketPort {
    fun save(ticket: Ticket)
}
