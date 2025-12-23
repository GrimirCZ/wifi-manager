package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ports.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.admin.persistence.mapper.AuthorizedDeviceMapper
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.stereotype.Repository

@Repository
class JpaAuthorizedDeviceRepositoryAdapter(
    val jpaRepository: AdminAuthorizedDeviceJpaRepository,
    val mapper: AuthorizedDeviceMapper,
) : FindAuthorizedDevicePort, SaveAuthorizedDevicePort {
    override fun findByMacAndTicketId(mac: String, ticketId: TicketId): AuthorizedDevice? {
        return jpaRepository.findByMacAndTicketId(mac, ticketId)?.let(mapper::authorizedDeviceToDomain)
    }

    override fun findByTicketId(ticketId: TicketId): List<AuthorizedDevice> {
        return jpaRepository.findAllByTicketId(ticketId.id).map(mapper::authorizedDeviceToDomain)
    }

    override fun save(authorizedDevice: AuthorizedDevice) {
        jpaRepository.save(mapper.authorizedDeviceToEntity(authorizedDevice))
    }
}