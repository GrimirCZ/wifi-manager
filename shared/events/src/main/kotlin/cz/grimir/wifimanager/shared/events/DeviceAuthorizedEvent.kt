package cz.grimir.wifimanager.shared.events

import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant

data class DeviceAuthorizedEvent(
    val ticketId: TicketId,
    val device: Device,
    val authorizedAt: Instant,
) {
    data class Device(
        val macAddress: String,
        val name: String?,
    )
}

