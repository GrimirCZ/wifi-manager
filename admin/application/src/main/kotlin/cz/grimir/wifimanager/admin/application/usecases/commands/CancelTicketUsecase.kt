package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.CancelTicketCommand
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.core.exceptions.CannotCancelInactiveTicket
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import cz.grimir.wifimanager.shared.events.TicketEndedEvent.Reason
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class CancelTicketUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
) {
    @Throws(CannotCancelInactiveTicket::class)
    fun cancel(command: CancelTicketCommand) {
        val ticket =
            findTicketPort.findById(command.ticketId)
                ?: error("Ticket with id ${command.ticketId} not found")

        if (
            ticket.authorId != command.user.userId &&
            !command.user.can(UserRole::canCancelOtherUsersTickets)
        ) {
            logger.warn {
                "Unauthorized ticket cancel attempt ticketId=${command.ticketId} userId=${command.user.userId}"
            }
            error("User ${command.user.userId} is not authorized to cancel ticket ${command.ticketId}")
        }

        ticket.cancel(
            command.user.userId,
        )

        saveTicketPort.save(ticket)

        eventPublisher.publish(
            TicketEndedEvent(
                ticketId = ticket.id,
                endedAt = Instant.now(),
                reason = Reason.MANUAL,
            ),
        )

        logger.info {
            "Ticket canceled ticketId=${ticket.id} userId=${command.user.userId}"
        }
    }
}
