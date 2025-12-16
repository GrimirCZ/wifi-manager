package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAuthorizationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CaptiveAuthorizationTokenJpaRepository : JpaRepository<CaptiveAuthorizationTokenEntity, UUID> {
    fun findByAccessCode(accessCode: String): CaptiveAuthorizationTokenEntity?
}
