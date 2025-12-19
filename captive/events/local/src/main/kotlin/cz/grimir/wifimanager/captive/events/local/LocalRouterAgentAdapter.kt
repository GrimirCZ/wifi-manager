package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import org.springframework.stereotype.Service

@Service
class LocalRouterAgentAdapter : RouterAgentPort {
    override fun allowClientAccess(macAddresses: List<String>) {
        // TODO: implement
    }

    override fun revokeClientAccess(macAddresses: List<String>) {
        // TODO: implement
    }
}
