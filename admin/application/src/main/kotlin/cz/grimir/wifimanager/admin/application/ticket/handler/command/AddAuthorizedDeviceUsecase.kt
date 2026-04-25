package cz.grimir.wifimanager.admin.application.ticket.handler.command

import cz.grimir.wifimanager.admin.application.ticket.command.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.ticket.port.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ticket.port.FindTicketPort
import cz.grimir.wifimanager.admin.application.ticket.port.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class AddAuthorizedDeviceUsecase(
    private val findTicketPort: FindTicketPort,
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort,
) {
    @Transactional
    fun add(command: AddAuthorizedDeviceCommand) {
        val existingDevice =
            findAuthorizedDevicePort.findByMacAndTicketId(command.deviceMacAddress, command.ticketId)

        if (existingDevice != null) {
            logger.info { "Device $existingDevice was already authorized, updating to $command." }
            saveAuthorizedDevicePort.save(
                existingDevice.copy(
                    displayName = command.displayName ?: existingDevice.displayName,
                    deviceName = command.deviceName ?: existingDevice.deviceName,
                ),
            )
        } else {
            saveAuthorizedDevicePort.save(
                AuthorizedDevice(
                    ticketId = command.ticketId,
                    mac = command.deviceMacAddress,
                    displayName = command.displayName ?: command.deviceName,
                    deviceName = command.deviceName,
                    wasAccessRevoked = false,
                ),
            )
        }
    }
}
