package cz.grimir.wifimanager.captive.application.usecase.queries

import cz.grimir.wifimanager.captive.application.network.NetworkUser
import cz.grimir.wifimanager.captive.application.ports.NetworkUserReadPort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class FindNetworkUserByUserIdUsecase(
    private val networkUserReadPort: NetworkUserReadPort,
) {
    fun find(userId: UserId): NetworkUser? = networkUserReadPort.findByUserId(userId)
}
