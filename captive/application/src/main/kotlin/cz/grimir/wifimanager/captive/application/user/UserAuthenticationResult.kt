package cz.grimir.wifimanager.captive.application.user

import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot

data class UserAuthenticationResult(
    val identity: UserIdentitySnapshot,
    val allowedDeviceCount: Int,
    val groups: Set<String> = emptySet(),
)
