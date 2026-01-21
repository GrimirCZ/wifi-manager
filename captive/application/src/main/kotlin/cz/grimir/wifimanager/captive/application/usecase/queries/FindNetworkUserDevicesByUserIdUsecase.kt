package cz.grimir.wifimanager.captive.application.usecase.queries

import cz.grimir.wifimanager.captive.application.network.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDevicesByUserIdUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(userId: UserId): List<NetworkUserDevice> = networkUserDeviceReadPort.findByUserId(userId)
}
