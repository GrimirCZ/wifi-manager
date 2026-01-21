package cz.grimir.wifimanager.captive.application.network

import cz.grimir.wifimanager.shared.core.UserId

data class CaptiveUserIdentityProfile(
    val userId: UserId,
    val email: String,
    val displayName: String,
)
