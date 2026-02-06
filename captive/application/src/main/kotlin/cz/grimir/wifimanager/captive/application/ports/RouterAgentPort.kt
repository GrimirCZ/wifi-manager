package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.shared.application.network.NetworkClient

interface RouterAgentPort {
    fun getClientInfo(ipAddress: String): ClientInfo?

    fun allowClientAccess(macAddresses: List<String>)

    fun revokeClientAccess(macAddresses: List<String>)

    fun listNetworkClients(): List<NetworkClient>
}
