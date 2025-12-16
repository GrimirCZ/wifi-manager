package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.policy.WhenTicketCreatedCreateCodeBasedAuthorizationPolicy
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TicketCreatedEventListener(
    private val policy: WhenTicketCreatedCreateCodeBasedAuthorizationPolicy,
) {
    @EventListener
    fun on(event: TicketCreatedEvent) {
        // TODO: implement (idempotency, ordering)
        policy.on(event)
    }
}
