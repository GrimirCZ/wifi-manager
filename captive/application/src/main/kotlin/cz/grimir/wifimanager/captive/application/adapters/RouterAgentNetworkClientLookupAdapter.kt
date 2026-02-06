package cz.grimir.wifimanager.captive.application.adapters

import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.shared.application.network.NetworkClient
import cz.grimir.wifimanager.shared.application.network.NetworkClientLookupPort
import org.springframework.stereotype.Service

@Service
class RouterAgentNetworkClientLookupAdapter(
    private val routerAgentPort: RouterAgentPort,
) : NetworkClientLookupPort {
    override fun listNetworkClients(): List<NetworkClient> = routerAgentPort.listNetworkClients()
}
