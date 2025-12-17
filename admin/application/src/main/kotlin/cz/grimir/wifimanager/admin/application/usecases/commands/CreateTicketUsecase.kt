package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.CreateTicketCommand
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import cz.grimir.wifimanager.admin.application.util.AccessCodeGenerator
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.exceptions.UserAlreadyHasActiveTickets
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CreateTicketUsecase(
    private val saveTicketPort: SaveTicketPort,
    private val findTicketPort: FindTicketPort,
    private val eventPublisher: AdminEventPublisher,
    private val accessCodeGenerator: AccessCodeGenerator,
) {
    @Transactional
    @Throws(UserAlreadyHasActiveTickets::class)
    fun create(command: CreateTicketCommand): Ticket {
        val existingUserTickets = findTicketPort.findByAuthorId(command.user.userId.id)
        val now = Instant.now()
        val activeUserTickets = existingUserTickets.filter { it.isActive(now) }

        if (!command.user.can(UserRole::canHaveMultipleTickets) && activeUserTickets.isNotEmpty()) {
            throw UserAlreadyHasActiveTickets(command.user.userId, activeUserTickets.map { it.id })
        }

        val validUntil = now.plusSeconds(command.duration.seconds)

        val ticket =
            Ticket(
                id = TicketId.new(),
                accessCode = accessCodeGenerator.generate(8),
                createdAt = now,
                validUntil = validUntil,
                wasCanceled = false,
                authorId = command.user.userId,
            )

        saveTicketPort.save(ticket)

        eventPublisher.publish(
            TicketCreatedEvent(
                id = ticket.id,
                accessCode = ticket.accessCode,
                createdAt = ticket.createdAt,
                validUntil = ticket.validUntil,
                author =
                    TicketCreatedEvent.Author(
                        userId = command.user.userId,
                        email = command.user.email,
                        displayName = command.user.username,
                    ),
            ),
        )

        return ticket
    }
}
