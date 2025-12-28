package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.shared.core.TicketId

interface FindAuthorizedDevicePort {
    fun findByMacAndTicketId(
        mac: String,
        ticketId: TicketId,
    ): AuthorizedDevice?

    fun findByTicketId(ticketId: TicketId): List<AuthorizedDevice>

    fun countByTicketId(ticketId: TicketId): Long
}
