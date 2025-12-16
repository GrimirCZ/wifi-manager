package cz.grimir.wifimanager.captive.application.policy

import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.stereotype.Service

@Service
class WhenDeviceAuthorizedAllowClientAccessPolicy(
    private val routerAgentPort: RouterAgentPort,
) {
    fun on(event: DeviceAuthorizedEvent) {
        // TODO: implement
        routerAgentPort.allowClientAccess(event.device.macAddress)
    }
}

