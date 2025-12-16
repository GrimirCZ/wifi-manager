package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.policy.WhenTicketEndedRemoveAuthorizationTokenPolicy
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TicketEndedEventListener(
    private val policy: WhenTicketEndedRemoveAuthorizationTokenPolicy,
) {
    @EventListener
    fun on(event: TicketEndedEvent) {
        // TODO: implement (idempotency, ordering)
        policy.on(event)
    }
}
