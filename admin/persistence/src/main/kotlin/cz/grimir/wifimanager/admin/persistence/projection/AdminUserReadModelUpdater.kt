package cz.grimir.wifimanager.admin.persistence.projection

import cz.grimir.wifimanager.admin.persistence.AdminUserIdentityJpaRepository
import cz.grimir.wifimanager.admin.persistence.entity.AdminUserIdentityEntity
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserReadModelUpdater(
    private val identityRepository: AdminUserIdentityJpaRepository,
) {
    @Transactional
    fun apply(event: UserCreatedOrUpdatedEvent) {
        val userId = event.userId.id

        identityRepository.save(
            AdminUserIdentityEntity(
                id = event.identityId,
                userId = userId,
                issuer = event.issuer,
                subject = event.subject,
                email = event.email,
                displayName = event.displayName,
                pictureUrl = event.pictureUrl,
                createdAt = event.createdAt,
                roles = event.roles.map(UserRole::name).toTypedArray(),
            ),
        )
    }
}
