package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.network.NetworkUser
import cz.grimir.wifimanager.captive.application.ports.NetworkUserReadPort
import cz.grimir.wifimanager.captive.application.ports.NetworkUserWritePort
import cz.grimir.wifimanager.captive.persistence.mapper.NetworkUserMapper
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaNetworkUserRepositoryAdapter(
    private val repository: NetworkUserJpaRepository,
    private val mapper: NetworkUserMapper,
) : NetworkUserReadPort,
    NetworkUserWritePort {
    override fun findByUserId(userId: UserId): NetworkUser? = repository.findByIdOrNull(userId.id)?.let(mapper::toDomain)

    override fun save(user: NetworkUser): NetworkUser = repository.save(mapper.toEntity(user)).let(mapper::toDomain)
}
