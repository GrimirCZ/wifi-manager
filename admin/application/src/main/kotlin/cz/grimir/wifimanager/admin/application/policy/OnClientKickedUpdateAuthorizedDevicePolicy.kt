package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.ports.FindAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ports.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import org.springframework.stereotype.Service

@Service
class OnClientKickedUpdateAuthorizedDevicePolicy(
    private val findAuthorizedDevicePort: FindAuthorizedDevicePort,
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort,
) {
    fun on(event: ClientKickedEvent) {
        val device = findAuthorizedDevicePort.findByMacAndTicketId(event.deviceMacAddress, event.ticketId) ?: return
        if (device.wasKicked) {
            return
        }

        saveAuthorizedDevicePort.save(
            device.copy(wasKicked = true),
        )
    }
}
