package cz.grimir.wifimanager.shared.application.network

interface NetworkClientLookupPort {
    fun listNetworkClients(): List<NetworkClient>
}
