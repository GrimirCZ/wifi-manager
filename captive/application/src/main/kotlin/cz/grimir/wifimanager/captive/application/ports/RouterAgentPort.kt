package cz.grimir.wifimanager.captive.application.ports

interface RouterAgentPort {
    fun getClientInfo(ipAddress: String): ClientInfo?

    fun allowClientAccess(macAddresses: List<String>)

    fun revokeClientAccess(macAddresses: List<String>)
}

data class ClientInfo(
    val macAddress: String,
    val hostname: String?,
)
