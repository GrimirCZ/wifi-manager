package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.application.ports.AuthorizationTokenRepository
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAuthorizationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaAuthorizationTokenRepository :
    AuthorizationTokenRepository,
    JpaRepository<UUID, CaptiveAuthorizationTokenEntity>
