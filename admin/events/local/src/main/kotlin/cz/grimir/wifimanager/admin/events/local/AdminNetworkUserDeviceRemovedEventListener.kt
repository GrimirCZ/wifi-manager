package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnNetworkUserDeviceRemovedRemoveUserDevicePolicy
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminNetworkUserDeviceRemovedEventListener(
    private val policy: OnNetworkUserDeviceRemovedRemoveUserDevicePolicy,
) {
    @EventListener
    fun on(event: NetworkUserDeviceRemovedEvent) {
        policy.on(event)
    }
}
