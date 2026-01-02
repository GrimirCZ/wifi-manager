package cz.grimir.wifimanager.captive.persistence.projection

import cz.grimir.wifimanager.captive.persistence.CaptiveUserIdentityJpaRepository
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveUserIdentityEntity
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CaptiveUserReadModelUpdater(
    private val identityRepository: CaptiveUserIdentityJpaRepository,
) {
    @Transactional
    fun apply(event: UserCreatedOrUpdatedEvent) {
        val existing = identityRepository.findByIdOrNull(event.identityId)
        val createdAt = existing?.createdAt ?: event.createdAt

        identityRepository.save(
            CaptiveUserIdentityEntity(
                id = event.identityId,
                userId = event.userId.id,
                issuer = event.issuer,
                subject = event.subject,
                displayName = event.displayName,
                email = event.email,
                pictureUrl = event.pictureUrl,
                roles = event.roles.map(UserRole::name).toTypedArray(),
                createdAt = createdAt,
            ),
        )
    }
}
