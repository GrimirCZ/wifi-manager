package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.persistence.entity.AdminTicketEntity
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class TicketMapper {
    fun ticketToDomain(entity: AdminTicketEntity): Ticket =
        Ticket(
            id = TicketId(entity.id),
            accessCode = entity.accessCode,
            createdAt = entity.createdAt,
            validUntil = entity.validUntil,
            wasCanceled = entity.wasCanceled,
            authorId = UserId(entity.authorId),
        )

    fun ticketToEntity(domain: Ticket): AdminTicketEntity =
        AdminTicketEntity(
            id = domain.id.id,
            accessCode = domain.accessCode,
            createdAt = domain.createdAt,
            validUntil = domain.validUntil,
            wasCanceled = domain.wasCanceled,
            authorId = domain.authorId.id,
        )
}

