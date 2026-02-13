package cz.grimir.wifimanager.admin.persistence

import cz.grimir.wifimanager.admin.persistence.entity.AdminUserDeviceEntity
import cz.grimir.wifimanager.admin.persistence.entity.AdminUserDeviceId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminUserDeviceJpaRepository : JpaRepository<AdminUserDeviceEntity, AdminUserDeviceId> {
    fun findAllByUserId(userId: UUID): List<AdminUserDeviceEntity>

    fun findByUserIdAndDeviceMac(
        userId: UUID,
        deviceMac: String,
    ): AdminUserDeviceEntity?

    fun deleteByUserIdAndDeviceMac(
        userId: UUID,
        deviceMac: String,
    )
}
