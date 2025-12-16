package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import org.springframework.stereotype.Service

@Service
class LocalRouterAgentAdapter : RouterAgentPort {
    override fun allowClientAccess(macAddress: String) {
        // TODO: implement
    }

    override fun revokeClientAccess(macAddress: String) {
        // TODO: implement
    }
}
