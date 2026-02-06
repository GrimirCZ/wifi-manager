package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.shared.application.network.NetworkClient
import cz.grimir.wifimanager.shared.application.network.NetworkClientLookupPort
import org.springframework.stereotype.Service

@Service
class AdminListNetworkClientsUsecase(
    private val networkClientLookupPort: NetworkClientLookupPort,
) {
    fun list(): List<NetworkClient> = networkClientLookupPort.listNetworkClients()
}
