package cz.grimir.wifimanager.captive.application.command

import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.shared.core.UserId

data class CompleteNetworkUserDeviceReauthCommand(
    val userId: UserId,
    val mac: String,
    val currentFingerprint: DeviceFingerprintProfile?,
)
