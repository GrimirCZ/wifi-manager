package cz.grimir.wifimanager.captive.application.ports

interface RouterAgentPort {
    fun allowClientAccess(macAddresses: List<String>)

    fun revokeClientAccess(macAddresses: List<String>)
}
