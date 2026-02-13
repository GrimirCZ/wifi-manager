package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.usecase.commands.RemoveNetworkUserDeviceUsecase
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NetworkUserDeviceDeauthorizeRequestedEventListener(
    private val removeNetworkUserDeviceUsecase: RemoveNetworkUserDeviceUsecase,
) {
    @EventListener
    fun on(event: NetworkUserDeviceDeauthorizeRequestedEvent) {
        removeNetworkUserDeviceUsecase.remove(event.userId, event.deviceMac)
    }
}
