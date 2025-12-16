package cz.grimir.wifimanager.captive.application.ports

interface RouterAgentPort {
    fun allowClientAccess(macAddress: String)

    fun revokeClientAccess(macAddress: String)
}
