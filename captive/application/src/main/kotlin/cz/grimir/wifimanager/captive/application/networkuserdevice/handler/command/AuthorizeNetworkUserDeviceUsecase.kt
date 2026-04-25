package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command

import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceAuthorizedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class AuthorizeNetworkUserDeviceUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
    private val deviceFingerprintService: DeviceFingerprintService,
) {
    @Transactional
    fun authorize(
        userId: UserId,
        mac: String,
        name: String?,
        hostname: String?,
        isRandomized: Boolean,
        fingerprintProfile: DeviceFingerprintProfile?,
    ) {
        val now = timeProvider.get()
        val fingerprintStatus = deviceFingerprintService.status(fingerprintProfile)
        val existing = networkUserDeviceReadPort.findByMac(mac)
        if (existing != null && existing.userId != userId) {
            throw DeviceOwnershipException("Device $mac is already authorized for another user.")
        }
        val device =
            existing?.copy(
                userId = userId,
                name = name,
                hostname = hostname,
                isRandomized = isRandomized,
                lastSeenAt = now,
                fingerprintProfile = deviceFingerprintService.enrichMissingSignals(existing.fingerprintProfile, fingerprintProfile),
                fingerprintStatus = fingerprintStatus,
                fingerprintVerifiedAt = now,
                reauthRequiredAt = null,
            ) ?: NetworkUserDevice(
                userId = userId,
                mac = mac,
                name = name,
                hostname = hostname,
                isRandomized = isRandomized,
                authorizedAt = now,
                lastSeenAt = now,
                fingerprintProfile = fingerprintProfile,
                fingerprintStatus = fingerprintStatus,
                fingerprintVerifiedAt = now,
                reauthRequiredAt = null,
            )
        networkUserDeviceWritePort.save(device)
        captiveEventPublisher.publish(
            NetworkUserDeviceAuthorizedEvent(
                userId = device.userId,
                deviceMac = device.mac,
                deviceName = device.name,
                hostname = device.hostname,
                isRandomized = device.isRandomized,
                authorizedAt = device.authorizedAt,
                lastSeenAt = device.lastSeenAt,
            ),
        )

        logger.info {
            "Device authorized userId=${userId} mac=${mac} deviceName=${hostname ?: "unknown"}"
        }
    }
}
