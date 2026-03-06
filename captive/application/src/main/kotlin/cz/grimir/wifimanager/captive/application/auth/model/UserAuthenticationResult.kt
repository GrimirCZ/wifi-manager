package cz.grimir.wifimanager.captive.application.auth.model

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot

data class UserAuthenticationResult(
    val identity: UserIdentitySnapshot,
    val allowedDeviceCount: Int,
    val groups: Set<String> = emptySet(),
)
