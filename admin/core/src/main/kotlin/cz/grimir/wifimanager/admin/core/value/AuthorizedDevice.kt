package cz.grimir.wifimanager.admin.core.value

import cz.grimir.wifimanager.shared.core.TicketId

data class AuthorizedDevice(
    /**
     * Device mac address.
     *
     * Serves as primary device identifier.
     */
    val mac: String,
    /**
     * Device hostname, if provided.
     */
    val name: String?,
    /**
     * ID of ticket that was used to authorize this device.
     */
    val ticketId: TicketId,
    /**
     * Whether the device's access was revoked by a user.
     */
    val wasAccessRevoked: Boolean,
)
