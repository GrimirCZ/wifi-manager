package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.query.ListNetworkClientsQuery
import cz.grimir.wifimanager.shared.application.network.NetworkClient
import cz.grimir.wifimanager.shared.application.network.NetworkClientLookupPort
import org.springframework.stereotype.Service

@Service
class AdminListNetworkClientsUsecase(
    private val networkClientLookupPort: NetworkClientLookupPort,
) {
    fun list(query: ListNetworkClientsQuery): List<NetworkClient> = networkClientLookupPort.listNetworkClients()
}
