package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveAuthorizationTokenEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CaptiveAuthorizationTokenJpaRepository : JpaRepository<CaptiveAuthorizationTokenEntity, UUID> {
    fun findByAccessCode(accessCode: String): CaptiveAuthorizationTokenEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select token
        from CaptiveAuthorizationTokenEntity token
        where token.accessCode = :accessCode
        """,
    )
    fun findByAccessCodeForAuthorization(accessCode: String): CaptiveAuthorizationTokenEntity?

    @Query(
        """
        select token
        from CaptiveAuthorizationTokenEntity token
        join token.authorizedDevices device
        where device.mac = :macAddress
        """,
    )
    fun findByAuthorizedDeviceMac(macAddress: String): CaptiveAuthorizationTokenEntity?

    @Query(
        """
        select distinct device.mac
        from CaptiveAuthorizationTokenEntity token
        join token.authorizedDevices device
        where device.reauthRequiredAt is null
        """,
    )
    fun findAllAuthorizedMacs(): List<String>
}
