package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.aggregates.Ticket

interface SaveTicketPort {
    fun save(ticket: Ticket)
}
