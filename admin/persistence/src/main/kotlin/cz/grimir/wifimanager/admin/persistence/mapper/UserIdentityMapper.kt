package cz.grimir.wifimanager.admin.persistence.mapper

import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.persistence.entity.AdminUserIdentityEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

/**
 * Maps persisted user identities (issuer+subject) to the application model.
 *
 * The unique constraint on (issuer, subject) is enforced at the DB level.
 */
@Component
class UserIdentityMapper {
    fun identityToApplication(entity: AdminUserIdentityEntity): UserIdentity =
        UserIdentity(
            id = entity.id,
            userId = UserId(entity.userId),
            issuer = entity.issuer,
            subject = entity.subject,
            email = entity.email,
            username = entity.username,
            firstName = entity.firstName,
            lastName = entity.lastName,
            pictureUrl = entity.pictureUrl,
            roles = entity.roles.map(UserRole::valueOf).toSet(),
            createdAt = entity.createdAt,
            lastLoginAt = entity.lastLoginAt,
        )

    fun identityToEntity(model: UserIdentity): AdminUserIdentityEntity =
        AdminUserIdentityEntity(
            id = model.id,
            userId = model.userId.id,
            issuer = model.issuer,
            subject = model.subject,
            email = model.email,
            username = model.username,
            firstName = model.firstName,
            lastName = model.lastName,
            pictureUrl = model.pictureUrl,
            roles = model.roles.map(UserRole::name).toTypedArray(),
            createdAt = model.createdAt,
            lastLoginAt = model.lastLoginAt,
        )
}
