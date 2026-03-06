package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.query

import cz.grimir.wifimanager.captive.application.networkuserdevice.model.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class FindNetworkUserDevicesByUserIdUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun find(userId: UserId): List<NetworkUserDevice> = networkUserDeviceReadPort.findByUserId(userId)
}
