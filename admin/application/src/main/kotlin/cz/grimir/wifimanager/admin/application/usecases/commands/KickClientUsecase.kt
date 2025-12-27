package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.KickDeviceCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import org.springframework.stereotype.Service

@Service
class KickClientUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
    fun kick(command: KickDeviceCommand) {
        val ticket =
            findTicketPort.findById(command.ticketId)
                ?: error("Ticket with id ${command.ticketId} not found")

        ticket.kickedMacAddresses.add(command.deviceMacAddress)
        saveTicketPort.save(ticket)

        eventPublisher.publish(
            ClientKickedEvent(
                ticketId = command.ticketId,
                deviceMacAddress = command.deviceMacAddress,
                kickedAt = timeProvider.get(),
            ),
        )
    }
}
