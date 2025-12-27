package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnClientKickedUpdateAuthorizedDevicePolicy
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminClientKickedEventListener(
    private val policy: OnClientKickedUpdateAuthorizedDevicePolicy,
) {
    @EventListener
    fun on(event: ClientKickedEvent) {
        policy.on(event)
    }
}
