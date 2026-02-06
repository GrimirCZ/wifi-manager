package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.application.queries.models.TicketWithDeviceCount
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.persistence.mapper.AdminTicketMapper
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class AdminJpaTicketRepositoryAdapter(
    private val jpaRepository: AdminTicketJpaRepository,
    private val mapper: AdminTicketMapper,
) : FindTicketPort,
    SaveTicketPort {
    override fun findById(id: TicketId): Ticket? = jpaRepository.findByIdOrNull(id.id)?.let(mapper::ticketToDomain)

    override fun findByAccessCode(accessCode: String): Ticket? = jpaRepository.findByAccessCode(accessCode)?.let(mapper::ticketToDomain)

    override fun findByAuthorId(authorId: UUID): List<Ticket> = jpaRepository.findByAuthorId(authorId).map(mapper::ticketToDomain)

    override fun findByAuthorIdWithDeviceCount(authorId: UUID): List<TicketWithDeviceCount> =
        jpaRepository.findByAuthorIdWithDeviceCount(authorId).map {
            TicketWithDeviceCount(
                ticket = mapper.ticketToDomain(it.ticket),
                deviceCount = it.deviceCount.toInt(),
            )
        }

    override fun findExpired(at: Instant): List<Ticket> =
        jpaRepository
            .findAllByWasCanceledFalseAndValidUntilLessThanEqual(at)
            .map(mapper::ticketToDomain)

    override fun save(ticket: Ticket) {
        jpaRepository.save(mapper.ticketToEntity(ticket))
    }
}
