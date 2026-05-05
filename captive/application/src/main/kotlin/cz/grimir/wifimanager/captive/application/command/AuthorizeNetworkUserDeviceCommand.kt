package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.shared.core.UserId

data class AuthorizeNetworkUserDeviceCommand(
    val userId: UserId,
    val mac: String,
    val name: String?,
    val hostname: String?,
    val isRandomized: Boolean,
    val fingerprintProfile: DeviceFingerprintProfile?,
)
