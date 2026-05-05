package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.shared.application.identity.model.UserIdentitySnapshot

data class UpsertNetworkUserOnLoginCommand(
    val identity: UserIdentitySnapshot,
    val allowedDeviceCount: Int,
)
