package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.query.model.CaptiveUserIdentityProfile
import cz.grimir.wifimanager.shared.core.UserId

interface CaptiveUserIdentityPort {
    fun findByUserId(userId: UserId): CaptiveUserIdentityProfile?
}
