package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.application.model.User
import cz.grimir.wifimanager.admin.persistence.entity.AdminUserEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun userToApplication(entity: AdminUserEntity): User =
        User(
            id = UserId(entity.id),
            email = entity.email,
            displayName = entity.displayName,
            pictureUrl = entity.pictureUrl,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            lastLoginAt = entity.lastLoginAt,
        )

    fun userToEntity(model: User): AdminUserEntity =
        AdminUserEntity(
            id = model.id.id,
            email = model.email,
            displayName = model.displayName,
            pictureUrl = model.pictureUrl,
            isActive = model.isActive,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            lastLoginAt = model.lastLoginAt,
        )
}

