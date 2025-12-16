package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.shared.TicketId

interface FindTicketPort {
    fun findById(id: TicketId): Ticket?
    fun findByAccessCode(accessCode: String): Ticket?
}

