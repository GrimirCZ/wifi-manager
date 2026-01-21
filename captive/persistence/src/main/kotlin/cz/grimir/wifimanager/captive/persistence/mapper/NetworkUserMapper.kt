package cz.grimir.wifimanager.captive.persistence.mapper

import cz.grimir.wifimanager.captive.application.network.NetworkUser
import cz.grimir.wifimanager.captive.persistence.entity.NetworkUserEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class NetworkUserMapper {
    fun toDomain(entity: NetworkUserEntity): NetworkUser =
        NetworkUser(
            userId = UserId(entity.userId),
            identityId = entity.identityId,
            allowedDeviceCount = entity.allowedDeviceCount,
            adminOverrideLimit = entity.adminOverrideLimit,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastLoginAt = entity.lastLoginAt,
        )

    fun toEntity(domain: NetworkUser): NetworkUserEntity =
        NetworkUserEntity(
            userId = domain.userId.id,
            identityId = domain.identityId,
            allowedDeviceCount = domain.allowedDeviceCount,
            adminOverrideLimit = domain.adminOverrideLimit,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            lastLoginAt = domain.lastLoginAt,
        )
}
