package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.KickDeviceCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.core.exceptions.UserNotAllowedToKickDevice
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

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

        val isOwner = ticket.authorId == command.user.userId
        val isAdmin = command.user.can(UserRole::canCancelOtherUsersTickets)
        if (!isOwner && !isAdmin) {
            logger.warn {
                "Unauthorized device kick attempt ticketId=${command.ticketId} userId=${command.user.userId} mac=${command.deviceMacAddress}"
            }
            throw UserNotAllowedToKickDevice(command.user.userId, command.ticketId)
        }

        ticket.kickedMacAddresses.add(command.deviceMacAddress)
        saveTicketPort.save(ticket)

        eventPublisher.publish(
            ClientKickedEvent(
                ticketId = command.ticketId,
                deviceMacAddress = command.deviceMacAddress,
                kickedAt = timeProvider.get(),
            ),
        )

        logger.info {
            "Device kicked ticketId=${command.ticketId} userId=${command.user.userId} mac=${command.deviceMacAddress}"
        }
    }
}
