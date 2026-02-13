package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.admin.persistence.entity.AdminUserDeviceEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class AdminUserDeviceMapper {
    fun toDomain(entity: AdminUserDeviceEntity): UserDevice =
        UserDevice(
            userId = UserId(entity.userId),
            mac = entity.deviceMac,
            name = entity.name,
            hostname = entity.hostname,
            isRandomized = entity.isRandomized,
            authorizedAt = entity.authorizedAt,
            lastSeenAt = entity.lastSeenAt,
        )

    fun toEntity(domain: UserDevice): AdminUserDeviceEntity =
        AdminUserDeviceEntity(
            userId = domain.userId.id,
            deviceMac = domain.mac,
            name = domain.name,
            hostname = domain.hostname,
            isRandomized = domain.isRandomized,
            authorizedAt = domain.authorizedAt,
            lastSeenAt = domain.lastSeenAt,
        )
}
