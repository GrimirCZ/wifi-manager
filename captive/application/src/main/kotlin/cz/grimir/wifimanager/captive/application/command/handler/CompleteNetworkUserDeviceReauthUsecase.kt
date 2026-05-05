package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.CompleteNetworkUserDeviceReauthCommand
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.shared.core.TimeProvider
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
    fun complete(command: CompleteNetworkUserDeviceReauthCommand): NetworkUserDevice? {
        val existing = networkUserDeviceReadPort.findByMac(command.mac) ?: return null
        require(existing.userId == command.userId) { "network user device ownership mismatch for mac=${command.mac}" }
        require(existing.reauthRequiredAt != null) { "network user device is not pending reauth for mac=${command.mac}" }

        val updated =
            existing.copy(
                fingerprintProfile = command.currentFingerprint,
                fingerprintStatus = deviceFingerprintService.status(command.currentFingerprint),
                fingerprintVerifiedAt = timeProvider.get(),
                reauthRequiredAt = null,
            )
        networkUserDeviceWritePort.save(updated)
        logger.info {
            "Completed network user device reauth userId=${command.userId.id} mac=${command.mac} fingerprintStatus=${updated.fingerprintStatus} fingerprintVerifiedAt=${updated.fingerprintVerifiedAt}"
        }
        return updated
    }
}
