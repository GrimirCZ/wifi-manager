package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserDeviceEntity
import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserDeviceId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface NetworkUserDeviceJpaRepository : JpaRepository<NetworkUserDeviceEntity, NetworkUserDeviceId> {
    fun findByDeviceMac(deviceMac: String): NetworkUserDeviceEntity?

    fun findAllByUserId(userId: UUID): List<NetworkUserDeviceEntity>

    fun countByUserId(userId: UUID): Long

    fun deleteByUserIdAndDeviceMac(
        userId: UUID,
        deviceMac: String,
    )

    @Modifying
    @Query(
        """
        update NetworkUserDeviceEntity device
        set device.lastSeenAt = CURRENT_TIMESTAMP
        where device.userId = :userId and device.deviceMac = :deviceMac
        """,
    )
    fun touchDevice(
        userId: UUID,
        deviceMac: String,
    ): Int
}
