package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.query.CountNetworkUserDevicesByUserIdQuery
import org.springframework.stereotype.Service

@Service
class CountNetworkUserDevicesByUserIdUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun count(query: CountNetworkUserDevicesByUserIdQuery): Long = networkUserDeviceReadPort.countByUserId(query.userId)
}
