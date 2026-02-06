package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.core.value.AllowedMac
import cz.grimir.wifimanager.admin.persistence.entity.AdminAllowedMacEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class AdminAllowedMacMapper {
    fun toDomain(entity: AdminAllowedMacEntity): AllowedMac =
        AllowedMac(
            mac = entity.mac,
            hostname = entity.hostname,
            ownerUserId = UserId(entity.ownerUserId),
            ownerDisplayName = entity.ownerDisplayName,
            ownerEmail = entity.ownerEmail,
            note = entity.note,
            validUntil = entity.validUntil,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    fun toEntity(domain: AllowedMac): AdminAllowedMacEntity =
        AdminAllowedMacEntity(
            mac = domain.mac,
            ownerUserId = domain.ownerUserId.id,
            ownerDisplayName = domain.ownerDisplayName,
            ownerEmail = domain.ownerEmail,
            note = domain.note,
            hostname = domain.hostname,
            validUntil = domain.validUntil,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
}
