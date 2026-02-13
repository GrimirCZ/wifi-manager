package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnNetworkUserDeviceAuthorizedUpsertUserDevicePolicy
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceAuthorizedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminNetworkUserDeviceAuthorizedEventListener(
    private val policy: OnNetworkUserDeviceAuthorizedUpsertUserDevicePolicy,
) {
    @EventListener
    fun on(event: NetworkUserDeviceAuthorizedEvent) {
        policy.on(event)
    }
}
