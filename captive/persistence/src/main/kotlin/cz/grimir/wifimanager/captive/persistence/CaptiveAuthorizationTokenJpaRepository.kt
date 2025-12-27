package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAuthorizationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CaptiveAuthorizationTokenJpaRepository : JpaRepository<CaptiveAuthorizationTokenEntity, UUID> {
    fun findByAccessCode(accessCode: String): CaptiveAuthorizationTokenEntity?

    @Query(
        """
        select token
        from CaptiveAuthorizationTokenEntity token
        join token.authorizedDevices device
        where device.mac = :macAddress
        """,
    )
    fun findByAuthorizedDeviceMac(macAddress: String): CaptiveAuthorizationTokenEntity?
}
