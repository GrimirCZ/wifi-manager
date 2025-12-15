package cz.grimir.wifimanager.captive.application.policy

import cz.grimir.wifimanager.captive.application.command.CreateCodeBasedAuthorizationCommand
import cz.grimir.wifimanager.captive.application.usecase.CreateCodeBasedAuthorizationUsecase
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import org.springframework.stereotype.Service

@Service
class WhenTicketCreatedCreateCodeBasedAuthorizationPolicy(
    val createUseCase: CreateCodeBasedAuthorizationUsecase,
) {
    fun on(event: TicketCreatedEvent) {
        createUseCase.create(
            CreateCodeBasedAuthorizationCommand(
                ticketId = event.id,
                accessCode = event.accessCode,
                createdAt = event.createdAt,
                validUntil = event.validUntil,
                userId = event.author.userId
            )
        )
    }
}
