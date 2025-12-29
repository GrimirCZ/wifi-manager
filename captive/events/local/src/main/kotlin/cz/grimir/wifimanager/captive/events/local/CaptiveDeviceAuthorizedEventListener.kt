package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.policy.WhenDeviceAuthorizedAllowClientAccessPolicy
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CaptiveDeviceAuthorizedEventListener(
    private val policy: WhenDeviceAuthorizedAllowClientAccessPolicy,
) {
    @EventListener
    fun on(event: DeviceAuthorizedEvent) {
        policy.on(event)
    }
}
