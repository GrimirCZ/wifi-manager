package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.application.network.CaptiveUserIdentityProfile
import cz.grimir.wifimanager.shared.core.UserId

interface CaptiveUserIdentityPort {
    fun findByUserId(userId: UserId): CaptiveUserIdentityProfile?
}
