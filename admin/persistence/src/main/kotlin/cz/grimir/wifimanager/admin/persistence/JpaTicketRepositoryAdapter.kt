package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.persistence.mapper.TicketMapper
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaTicketRepositoryAdapter(
    private val jpaRepository: AdminTicketJpaRepository,
    private val mapper: TicketMapper,
) : FindTicketPort,
    SaveTicketPort {
    override fun findById(id: TicketId): Ticket? = jpaRepository.findByIdOrNull(id.id)?.let(mapper::ticketToDomain)

    override fun findByAccessCode(accessCode: String): Ticket? =
        jpaRepository.findByAccessCode(accessCode)?.let(mapper::ticketToDomain)

    override fun save(ticket: Ticket) {
        jpaRepository.save(mapper.ticketToEntity(ticket))
    }
}
