package cz.grimir.wifimanager.captive.application.policy

import cz.grimir.wifimanager.captive.application.command.RemoveAuthorizationTokenCommand
import cz.grimir.wifimanager.captive.application.usecase.RemoveAuthorizationTokenUsecase
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import org.springframework.stereotype.Service

@Service
class WhenTicketEndedRemoveAuthorizationTokenPolicy(
    private val removeAuthorizationTokenUsecase: RemoveAuthorizationTokenUsecase,
) {
    fun on(event: TicketEndedEvent) {
        // TODO: implement
        removeAuthorizationTokenUsecase.remove(
            RemoveAuthorizationTokenCommand(ticketId = event.ticketId)
        )
    }
}

