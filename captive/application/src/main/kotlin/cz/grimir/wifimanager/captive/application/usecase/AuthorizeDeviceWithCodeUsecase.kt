package cz.grimir.wifimanager.captive.application.usecase

import cz.grimir.wifimanager.captive.application.command.AuthorizeDeviceWithCodeCommand
import cz.grimir.wifimanager.captive.application.ports.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.core.exceptions.InvalidAccessCodeException
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AuthorizeDeviceWithCodeUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val eventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    fun authorize(command: AuthorizeDeviceWithCodeCommand) {
        val token =
            findAuthorizationTokenPort.findByAccessCode(command.accessCode)
                ?: throw InvalidAccessCodeException(command.accessCode, command.device.mac)

        token.authorizeDevice(command.device)
        modifyAuthorizationTokenPort.save(token)

        eventPublisher.publish(
            DeviceAuthorizedEvent(
                ticketId = token.id,
                device =
                    DeviceAuthorizedEvent.Device(
                        macAddress = command.device.mac,
                        name = command.device.name,
                    ),
                authorizedAt = timeProvider.get(),
            ),
        )

        logger.info {
            "Device authorized ticketId=${token.id} mac=${command.device.mac} name=${command.device.name ?: "unknown"} validUntil=${token.validUntil}"
        }
    }
}
