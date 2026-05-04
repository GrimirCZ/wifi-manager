package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.command.RequestUserDeviceDeauthorizationCommand
import cz.grimir.wifimanager.admin.application.port.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.port.FindUserDevicePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class RequestUserDeviceDeauthorizationUsecase(
    private val findUserDevicePort: FindUserDevicePort,
    private val adminEventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
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
