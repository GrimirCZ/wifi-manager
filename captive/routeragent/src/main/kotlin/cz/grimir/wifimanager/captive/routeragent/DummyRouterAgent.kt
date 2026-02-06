package cz.grimir.wifimanager.captive.routeragent

import cz.grimir.wifimanager.captive.application.ports.ClientInfo
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.shared.application.network.NetworkClient
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class DummyRouterAgent(
    private val properties: DummyRouterAgentProperties,
) : RouterAgentPort {
    init {
        logger.info { "Dummy router agent initialized with properties: $properties" }
    }

    override fun getClientInfo(ipAddress: String): ClientInfo {
        val macAddress = properties.clientMacAddressByIp[ipAddress] ?: properties.defaultClientMacAddress
        val hostname = properties.clientHostnameByIp[ipAddress] ?: properties.defaultClientHostname
        logger.info { "Dummy router agent getClientInfo ip=$ipAddress mac=$macAddress hostname=$hostname" }
        return ClientInfo(macAddress = macAddress, hostname = hostname)
    }

    override fun allowClientAccess(macAddresses: List<String>) {
        logger.info { "Dummy router agent allowClientAccess macAddresses=$macAddresses" }
    }

    override fun revokeClientAccess(macAddresses: List<String>) {
        logger.info { "Dummy router agent revokeClientAccess macAddresses=$macAddresses" }
    }

    override fun listNetworkClients(): List<NetworkClient> {
        val ipAddress = properties.clientMacAddressByIp.keys.firstOrNull() ?: "127.0.0.1"
        val macAddress = properties.clientMacAddressByIp[ipAddress] ?: properties.defaultClientMacAddress
        val hostname = properties.clientHostnameByIp[ipAddress] ?: properties.defaultClientHostname
        logger.info { "Dummy router agent listNetworkClients ip=$ipAddress mac=$macAddress hostname=$hostname" }
        return listOf(
            NetworkClient(
                macAddress = macAddress,
                ipAddresses = listOf(ipAddress),
                hostname = hostname,
                allowed = false,
            ),
        )
    }
}
