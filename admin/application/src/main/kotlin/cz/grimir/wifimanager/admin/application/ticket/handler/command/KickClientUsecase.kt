package cz.grimir.wifimanager.admin.application.ticket.handler.command

import cz.grimir.wifimanager.admin.application.shared.port.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ticket.command.KickDeviceCommand
import cz.grimir.wifimanager.admin.application.ticket.port.FindTicketPort
import cz.grimir.wifimanager.admin.application.ticket.port.SaveTicketPort
import cz.grimir.wifimanager.admin.core.exceptions.UserNotAllowedToKickDevice
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class KickClientUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
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
