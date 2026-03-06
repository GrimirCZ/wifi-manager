package cz.grimir.wifimanager.captive.application.identity.port

import cz.grimir.wifimanager.captive.application.identity.model.CaptiveUserIdentityProfile
import cz.grimir.wifimanager.shared.core.UserId

interface CaptiveUserIdentityPort {
    fun findByUserId(userId: UserId): CaptiveUserIdentityProfile?
}
