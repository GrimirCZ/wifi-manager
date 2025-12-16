package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnDeviceAuthorizedUpdateTicketPolicy
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminDeviceAuthorizedEventListener(
    private val policy: OnDeviceAuthorizedUpdateTicketPolicy,
) {
    @EventListener
    fun on(event: DeviceAuthorizedEvent) {
        // TODO: implement (validation, idempotency)
        policy.on(event)
    }
}

