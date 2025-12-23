package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnDeviceAuthorizedCreateAuthorizedDevicePolicy
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminDeviceAuthorizedEventListener(
    private val policy: OnDeviceAuthorizedCreateAuthorizedDevicePolicy,
) {
    @EventListener
    fun on(event: DeviceAuthorizedEvent) {
        policy.on(event)
    }
}
