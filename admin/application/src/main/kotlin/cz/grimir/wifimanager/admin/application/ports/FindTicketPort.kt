package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.admin.application.queries.models.TicketWithDeviceCount
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant
import java.util.UUID

interface FindTicketPort {
    fun findById(id: TicketId): Ticket?

    fun findByAccessCode(accessCode: String): Ticket?

    fun findByAuthorId(authorId: UUID): List<Ticket>

    fun findByAuthorIdWithDeviceCount(authorId: UUID): List<TicketWithDeviceCount>

    fun findExpired(at: Instant): List<Ticket>
}
