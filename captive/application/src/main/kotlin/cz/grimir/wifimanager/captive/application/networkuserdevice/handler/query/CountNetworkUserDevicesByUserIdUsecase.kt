package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query

import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class CountNetworkUserDevicesByUserIdUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun count(userId: UserId): Long = networkUserDeviceReadPort.countByUserId(userId)
}
