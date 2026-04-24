package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command.RemoveNetworkUserDeviceUsecase
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NetworkUserDeviceDeauthorizeRequestedEventListener(
    private val removeNetworkUserDeviceUsecase: RemoveNetworkUserDeviceUsecase,
) {
    @EventListener
    fun on(event: NetworkUserDeviceDeauthorizeRequestedEvent) {
        removeNetworkUserDeviceUsecase.remove(event.userId, MacAddressNormalizer.normalize(event.deviceMac))
    }
}
