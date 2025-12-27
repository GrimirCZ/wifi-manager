package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ports.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import org.springframework.stereotype.Service

@Service
class OnClientAccessRevokedUpdateAuthorizedDevicePolicy(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort,
) {
    fun on(event: ClientAccessRevokedEvent) {
        val device = findAuthorizedDevicePort.findByMacAndTicketId(event.deviceMacAddress, event.ticketId) ?: return
        if (device.wasAccessRevoked) {
            return
        }

        saveAuthorizedDevicePort.save(
            device.copy(wasAccessRevoked = true),
        )
    }
}
