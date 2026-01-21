package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveUserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CaptiveUserIdentityJpaRepository : JpaRepository<CaptiveUserIdentityEntity, UUID> {
    fun findByUserId(userId: UUID): CaptiveUserIdentityEntity?
}
