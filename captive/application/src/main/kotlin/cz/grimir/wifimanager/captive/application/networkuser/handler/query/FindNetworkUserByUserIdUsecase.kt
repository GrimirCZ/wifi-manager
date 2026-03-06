package cz.grimir.wifimanager.captive.application.networkuser.handler.query

import cz.grimir.wifimanager.captive.application.networkuser.model.NetworkUser
import cz.grimir.wifimanager.captive.application.networkuser.port.NetworkUserReadPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class FindNetworkUserByUserIdUsecase(
    private val networkUserReadPort: NetworkUserReadPort,
) {
    fun find(userId: UserId): NetworkUser? = networkUserReadPort.findByUserId(userId)
}
