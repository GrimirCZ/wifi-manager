package cz.grimir.wifimanager.captive.application.usecase.queries

import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class CountNetworkUserDevicesByUserIdUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
) {
    fun count(userId: UserId): Long = networkUserDeviceReadPort.countByUserId(userId)
}
