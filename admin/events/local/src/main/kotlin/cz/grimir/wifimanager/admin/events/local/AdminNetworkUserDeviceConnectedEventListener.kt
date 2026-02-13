package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.policy.OnNetworkUserDeviceConnectedConnectUserDevicePolicy
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceConnectedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AdminNetworkUserDeviceConnectedEventListener(
    private val policy: OnNetworkUserDeviceConnectedConnectUserDevicePolicy,
) {
    @EventListener
    fun on(event: NetworkUserDeviceConnectedEvent) {
        policy.on(event)
    }
}
