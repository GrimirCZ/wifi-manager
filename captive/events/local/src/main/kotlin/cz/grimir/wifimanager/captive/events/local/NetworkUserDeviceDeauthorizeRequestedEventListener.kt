package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.command.handler.RemoveNetworkUserDeviceUsecase
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class NetworkUserDeviceDeauthorizeRequestedEventListener(
    private val removeNetworkUserDeviceUsecase: RemoveNetworkUserDeviceUsecase,
) {
    @EventListener
    fun on(event: NetworkUserDeviceDeauthorizeRequestedEvent) {
        val mac = MacAddressNormalizer.normalize(event.deviceMac)
        logger.info {
            "Network user device deauthorization requested userId=${event.userId} mac=$mac requestedBy=${event.requestedByUserId}"
        }
        removeNetworkUserDeviceUsecase.remove(event.userId, mac)
    }
}
