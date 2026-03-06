package cz.grimir.wifimanager.captive.application.policy

import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.stereotype.Service

@Service
class WhenDeviceAuthorizedAllowClientAccessPolicy(
    private val routerAgentPort: RouterAgentPort,
) {
    fun on(event: DeviceAuthorizedEvent) {
        routerAgentPort.allowClientAccess(listOf(event.device.macAddress))
    }
}
