package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.usecases.commands.ConnectUserDeviceUsecase
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceConnectedEvent
import org.springframework.stereotype.Service

@Service
class OnNetworkUserDeviceConnectedConnectUserDevicePolicy(
    private val connectUserDeviceUsecase: ConnectUserDeviceUsecase,
) {
    fun on(event: NetworkUserDeviceConnectedEvent) {
        connectUserDeviceUsecase.connect(
            userId = event.userId,
            mac = event.deviceMac,
            connectedAt = event.connectedAt,
        )
    }
}
