package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.network.CaptiveUserIdentityProfile
import cz.grimir.wifimanager.captive.application.ports.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.persistence.mapper.CaptiveUserIdentityMapper
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Repository

@Repository
class JpaCaptiveUserIdentityRepositoryAdapter(
    private val repository: CaptiveUserIdentityJpaRepository,
    private val mapper: CaptiveUserIdentityMapper,
) : CaptiveUserIdentityPort {
    override fun findByUserId(userId: UserId): CaptiveUserIdentityProfile? = repository.findByUserId(userId.id)?.let(mapper::toProfile)
}
