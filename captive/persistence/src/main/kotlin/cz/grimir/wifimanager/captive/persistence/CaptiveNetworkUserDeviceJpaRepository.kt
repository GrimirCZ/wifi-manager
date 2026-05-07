package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserDeviceEntity
import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserDeviceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface CaptiveNetworkUserDeviceJpaRepository : JpaRepository<NetworkUserDeviceEntity, NetworkUserDeviceId> {
    fun findByDeviceMac(deviceMac: String): NetworkUserDeviceEntity?

    @Query("select device.deviceMac from NetworkUserDeviceEntity device where device.reauthRequiredAt is null")
    fun findAllAuthorizedMacs(): List<String>

    fun findAllByUserId(userId: UUID): List<NetworkUserDeviceEntity>

    fun countByUserId(userId: UUID): Long

    fun deleteByUserIdAndDeviceMac(
        userId: UUID,
        deviceMac: String,
    )

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update NetworkUserDeviceEntity d
        set d.lastSeenAt = :lastSeenAt
        where d.userId = :userId and d.deviceMac = :deviceMac and d.lastSeenAt < :lastSeenAt
        """,
    )
    fun updateLastSeenAtIfNewer(
        userId: UUID,
        deviceMac: String,
        lastSeenAt: Instant,
    ): Int
}
