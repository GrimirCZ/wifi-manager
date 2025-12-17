package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.persistence.entity.AdminUserIdentityEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class UserIdentityMapper {
    fun identityToApplication(entity: AdminUserIdentityEntity): UserIdentity =
        UserIdentity(
            id = entity.id,
            userId = UserId(entity.userId),
            issuer = entity.issuer,
            subject = entity.subject,
            emailAtProvider = entity.emailAtProvider,
            providerUsername = entity.providerUsername,
            createdAt = entity.createdAt,
            lastLoginAt = entity.lastLoginAt,
        )

    fun identityToEntity(model: UserIdentity): AdminUserIdentityEntity =
        AdminUserIdentityEntity(
            id = model.id,
            userId = model.userId.id,
            issuer = model.issuer,
            subject = model.subject,
            emailAtProvider = model.emailAtProvider,
            providerUsername = model.providerUsername,
            createdAt = model.createdAt,
            lastLoginAt = model.lastLoginAt,
        )
}

