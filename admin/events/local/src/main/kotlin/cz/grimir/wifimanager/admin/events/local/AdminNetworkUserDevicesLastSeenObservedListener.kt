package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnNetworkUserDevicesLastSeenObservedPolicy
import cz.grimir.wifimanager.shared.events.NetworkUserDevicesLastSeenObservedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminNetworkUserDevicesLastSeenObservedListener(
    private val policy: OnNetworkUserDevicesLastSeenObservedPolicy,
) {
    @EventListener
    fun on(event: NetworkUserDevicesLastSeenObservedEvent) {
        policy.on(event)
    }
}
