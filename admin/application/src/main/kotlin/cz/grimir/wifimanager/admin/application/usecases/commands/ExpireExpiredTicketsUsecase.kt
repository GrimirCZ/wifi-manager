package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.ExpireTicketsCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class ExpireExpiredTicketsUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
) {
    @Transactional
    fun expireExpiredTickets(command: ExpireTicketsCommand): Int {
        val expiredTickets = findTicketPort.findExpired(command.at)

        expiredTickets.forEach { ticket ->
            if (ticket.wasCanceled) {
                return@forEach
            }

            ticket.wasCanceled = true
            saveTicketPort.save(ticket)

            logger.info { "Ticket $ticket has expired and is being marked as canceled." }

            eventPublisher.publish(
                TicketEndedEvent(
                    ticketId = ticket.id,
                    endedAt = command.at,
                    reason = TicketEndedEvent.Reason.EXPIRED,
                ),
            )
        }

        return expiredTickets.size
    }
}
