package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.AuthorizeNetworkUserDeviceCommand
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.support.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.shared.core.TimeProvider
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
    fun authorize(command: AuthorizeNetworkUserDeviceCommand) {
        val now = timeProvider.get()
        val fingerprintStatus = deviceFingerprintService.status(command.fingerprintProfile)
        val existing = networkUserDeviceReadPort.findByMac(command.mac)
        if (existing != null && existing.userId != command.userId) {
            throw DeviceOwnershipException("Device ${command.mac} is already authorized for another user.")
        }
        val device =
            existing?.copy(
                userId = command.userId,
                name = command.name,
                hostname = command.hostname,
                isRandomized = command.isRandomized,
                lastSeenAt = now,
                fingerprintProfile = deviceFingerprintService.enrichMissingSignals(existing.fingerprintProfile, command.fingerprintProfile),
                fingerprintStatus = fingerprintStatus,
                fingerprintVerifiedAt = now,
                reauthRequiredAt = null,
            ) ?: NetworkUserDevice(
                userId = command.userId,
                mac = command.mac,
                name = command.name,
                hostname = command.hostname,
                isRandomized = command.isRandomized,
                authorizedAt = now,
                lastSeenAt = now,
                fingerprintProfile = command.fingerprintProfile,
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
            "Device authorized userId=${command.userId} mac=${command.mac} deviceName=${command.hostname ?: "unknown"}"
        }
    }
}
