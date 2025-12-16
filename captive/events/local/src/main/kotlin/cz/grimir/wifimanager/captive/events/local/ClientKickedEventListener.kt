package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.policy.WhenClientKickedRevokeClientAccessPolicy
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ClientKickedEventListener(
    private val policy: WhenClientKickedRevokeClientAccessPolicy,
) {
    @EventListener
    fun on(event: ClientKickedEvent) {
        // TODO: implement (idempotency, ordering)
        policy.on(event)
    }
}

