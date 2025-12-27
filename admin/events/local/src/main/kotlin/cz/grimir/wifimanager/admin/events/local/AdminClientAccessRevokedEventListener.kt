package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnClientAccessRevokedUpdateAuthorizedDevicePolicy
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminClientAccessRevokedEventListener(
    private val policy: OnClientAccessRevokedUpdateAuthorizedDevicePolicy,
) {
    @EventListener
    fun on(event: ClientAccessRevokedEvent) {
        policy.on(event)
    }
}
