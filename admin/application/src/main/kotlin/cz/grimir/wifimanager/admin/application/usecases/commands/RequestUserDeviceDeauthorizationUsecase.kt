package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.RequestUserDeviceDeauthorizationCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindUserDevicePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class RequestUserDeviceDeauthorizationUsecase(
    private val findUserDevicePort: FindUserDevicePort,
    private val adminEventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
    fun request(command: RequestUserDeviceDeauthorizationCommand) {
        val device = findUserDevicePort.findByUserIdAndMac(command.userId, command.deviceMac) ?: return
        val now = timeProvider.get()
        try {
            adminEventPublisher.publish(
                NetworkUserDeviceDeauthorizeRequestedEvent(
                    userId = device.userId,
                    deviceMac = device.mac,
                    requestedByUserId = command.requestedBy.userId,
                    requestedAt = now,
                ),
            )
            logger.info {
                "Disconnect requested requestedBy=${command.requestedBy.userId} targetUser=${device.userId} mac=${device.mac}"
            }
        } catch (ex: RuntimeException) {
            logger.warn(ex) {
                "Disconnect request failed requestedBy=${command.requestedBy.userId} targetUser=${device.userId} mac=${device.mac}"
            }
            throw ex
        }
    }
}
