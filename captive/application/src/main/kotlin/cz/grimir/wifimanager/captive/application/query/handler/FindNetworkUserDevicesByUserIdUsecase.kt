package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.query.FindNetworkUserDevicesByUserIdQuery
import cz.grimir.wifimanager.captive.application.query.model.NetworkUserDevice
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDevicesByUserIdUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(query: FindNetworkUserDevicesByUserIdQuery): List<NetworkUserDevice> = networkUserDeviceReadPort.findByUserId(query.userId)
}
