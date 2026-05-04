package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.query.model.CaptiveUserIdentityProfile
import cz.grimir.wifimanager.captive.application.port.CaptiveUserIdentityPort
import cz.grimir.wifimanager.captive.persistence.mapper.CaptiveUserIdentityMapper
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Repository

@Repository
class CaptiveJpaUserIdentityRepositoryAdapter(
    private val repository: CaptiveUserIdentityJpaRepository,
    private val mapper: CaptiveUserIdentityMapper,
) : CaptiveUserIdentityPort {
    override fun findByUserId(userId: UserId): CaptiveUserIdentityProfile? = repository.findByUserId(userId.id)?.let(mapper::toProfile)
}
