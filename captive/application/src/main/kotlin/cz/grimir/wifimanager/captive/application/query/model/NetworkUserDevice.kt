package cz.grimir.wifimanager.captive.application.query.model

import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import cz.grimir.wifimanager.shared.core.UserId
import java.time.Instant

data class NetworkUserDevice(
    val userId: UserId,
    val mac: String,
    val name: String?,
    val hostname: String?,
    val isRandomized: Boolean,
    val authorizedAt: Instant,
    val lastSeenAt: Instant,
    val fingerprintProfile: DeviceFingerprintProfile?,
    val fingerprintStatus: DeviceFingerprintStatus,
    val fingerprintVerifiedAt: Instant?,
    val reauthRequiredAt: Instant?,
)