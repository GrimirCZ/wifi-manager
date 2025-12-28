package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.captive.persistence.mapper.AuthorizationTokenMapper
import cz.grimir.wifimanager.shared.core.TicketId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaAuthorizationTokenRepositoryAdapter(
    private val jpaRepository: CaptiveAuthorizationTokenJpaRepository,
    private val mapper: AuthorizationTokenMapper,
) : FindAuthorizationTokenPort,
    ModifyAuthorizationTokenPort {
    override fun findByTicketId(ticketId: TicketId): AuthorizationToken? =
        jpaRepository.findByIdOrNull(ticketId.id)?.let(mapper::tokenToDomain)

    override fun findByAccessCode(accessCode: String): AuthorizationToken? =
        jpaRepository.findByAccessCode(accessCode)?.let(mapper::tokenToDomain)

    override fun findByAuthorizedDeviceMac(macAddress: String): AuthorizationToken? =
        jpaRepository.findByAuthorizedDeviceMac(macAddress)?.let(mapper::tokenToDomain)

    override fun findAllAuthorizedDeviceMacs(): List<String> = jpaRepository.findAllAuthorizedDeviceMacs()

    override fun save(token: AuthorizationToken) {
        jpaRepository.save(mapper.tokenToEntity(token))
    }

    override fun deleteByTicketId(ticketId: TicketId) {
        jpaRepository.deleteById(ticketId.id)
    }
}
