package cz.grimir.wifimanager.captive.application.query.handler

import cz.grimir.wifimanager.captive.application.port.NetworkUserReadPort
import cz.grimir.wifimanager.captive.application.query.FindNetworkUserByUserIdQuery
import cz.grimir.wifimanager.captive.application.query.model.NetworkUser
import org.springframework.stereotype.Service

@Service
class FindNetworkUserByUserIdUsecase(
    private val networkUserReadPort: NetworkUserReadPort,
) {
    fun find(query: FindNetworkUserByUserIdQuery): NetworkUser? = networkUserReadPort.findByUserId(query.userId)
}
