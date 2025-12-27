package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AddAuthorizedDeviceUsecase(
    private val findTicketPort: FindTicketPort,
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort,
) {
    fun add(command: AddAuthorizedDeviceCommand) {
        val ticket = findTicketPort.findById(command.ticketId)
        if (ticket == null) {
            logger.warn { "Ticket with id ${command.ticketId} not found. Cannot add authorized device $command." }
            return
        }

        val wasKicked = ticket.kickedMacAddresses.contains(command.deviceMacAddress)
        val existingDevice =
            findAuthorizedDevicePort.findByMacAndTicketId(command.deviceMacAddress, command.ticketId)

        if (existingDevice != null) {
            logger.info { "Device $existingDevice was already authorized, updating to $command." }
            saveAuthorizedDevicePort.save(
                existingDevice.copy(
                    name = command.deviceName ?: existingDevice.name,
                    wasKicked = wasKicked,
                ),
            )
        } else {
            saveAuthorizedDevicePort.save(
                AuthorizedDevice(
                    ticketId = command.ticketId,
                    mac = command.deviceMacAddress,
                    name = command.deviceName,
                    wasKicked = wasKicked,
                ),
            )
        }
    }
}
