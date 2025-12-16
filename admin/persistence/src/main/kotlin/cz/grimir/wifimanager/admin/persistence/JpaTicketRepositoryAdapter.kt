package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.persistence.entity.AdminTicketEntity
import cz.grimir.wifimanager.shared.TicketId
import org.springframework.stereotype.Repository

@Repository
class JpaTicketRepositoryAdapter(
    private val jpaRepository: AdminTicketJpaRepository,
) : FindTicketPort, SaveTicketPort {
    override fun findById(id: TicketId): Ticket? {
        // TODO: implement (map entity -> domain)
        val entity = jpaRepository.findById(id.id).orElse(null) ?: return null
        return entity.toDomain()
    }

    override fun findByAccessCode(accessCode: String): Ticket? {
        // TODO: implement (map entity -> domain)
        val entity = jpaRepository.findByAccessCode(accessCode) ?: return null
        return entity.toDomain()
    }

    override fun save(ticket: Ticket) {
        // TODO: implement (map domain -> entity)
        jpaRepository.save(ticket.toEntity())
    }
}

private fun AdminTicketEntity.toDomain(): Ticket {
    // TODO: implement
    return TODO("implement")
}

private fun Ticket.toEntity(): AdminTicketEntity {
    // TODO: implement
    return TODO("implement")
}

