package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command

import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class CompleteNetworkUserDeviceReauthUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val deviceFingerprintService: DeviceFingerprintService,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun complete(
        userId: UserId,
        mac: String,
        currentFingerprint: DeviceFingerprintProfile?,
    ): NetworkUserDevice? {
        val existing = networkUserDeviceReadPort.findByMac(mac) ?: return null
        require(existing.userId == userId) { "network user device ownership mismatch for mac=$mac" }
        require(existing.reauthRequiredAt != null) { "network user device is not pending reauth for mac=$mac" }

        val updated =
            existing.copy(
                fingerprintProfile = currentFingerprint,
                fingerprintStatus = deviceFingerprintService.status(currentFingerprint),
                fingerprintVerifiedAt = timeProvider.get(),
                reauthRequiredAt = null,
            )
        networkUserDeviceWritePort.save(updated)
        logger.info {
            "Completed network user device reauth userId=${userId.id} mac=$mac fingerprintStatus=${updated.fingerprintStatus} fingerprintVerifiedAt=${updated.fingerprintVerifiedAt}"
        }
        return updated
    }
}
