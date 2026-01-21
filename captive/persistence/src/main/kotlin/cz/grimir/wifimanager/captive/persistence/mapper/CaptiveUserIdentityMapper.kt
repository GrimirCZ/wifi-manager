package cz.grimir.wifimanager.captive.persistence.mapper

import cz.grimir.wifimanager.captive.application.network.CaptiveUserIdentityProfile
import cz.grimir.wifimanager.captive.persistence.entity.CaptiveUserIdentityEntity
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Component

@Component
class CaptiveUserIdentityMapper {
    fun toProfile(entity: CaptiveUserIdentityEntity): CaptiveUserIdentityProfile =
        CaptiveUserIdentityProfile(
            userId = UserId(entity.userId),
            email = entity.email,
            displayName = entity.displayName,
        )
}
