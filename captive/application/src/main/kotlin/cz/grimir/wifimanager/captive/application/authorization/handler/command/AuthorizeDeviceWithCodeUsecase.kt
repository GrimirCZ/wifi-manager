package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.authorization.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.devicefingerprint.DeviceFingerprintService
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.core.exceptions.InvalidAccessCodeException
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class AuthorizeDeviceWithCodeUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val eventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
    private val deviceFingerprintService: DeviceFingerprintService,
) {
    @Transactional
    fun authorize(command: AuthorizeDeviceWithCodeCommand) {
        val now = timeProvider.get()
        val token =
            findAuthorizationTokenPort.findByAccessCode(command.accessCode)
                ?: throw InvalidAccessCodeException(command.accessCode, command.device.mac)

        val acceptedDevice =
            command.device.copy(
                fingerprintStatus = deviceFingerprintService.status(command.device.fingerprintProfile),
                fingerprintVerifiedAt = now,
                reauthRequiredAt = null,
            )
        token.authorizeDevice(acceptedDevice)
        modifyAuthorizationTokenPort.save(token)

        eventPublisher.publish(
            DeviceAuthorizedEvent(
                ticketId = token.id,
                device =
                    DeviceAuthorizedEvent.Device(
                        macAddress = acceptedDevice.mac,
                        displayName = acceptedDevice.displayName,
                        deviceName = acceptedDevice.deviceName,
                    ),
                authorizedAt = now,
            ),
        )

        logger.info {
            "Device authorized ticketId=${token.id} mac=${acceptedDevice.mac} displayName=${acceptedDevice.displayName ?: "unknown"} deviceName=${acceptedDevice.deviceName ?: "unknown"} validUntil=${token.validUntil}"
        }
    }
}
