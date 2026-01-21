package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NetworkUserJpaRepository : JpaRepository<NetworkUserEntity, UUID>
