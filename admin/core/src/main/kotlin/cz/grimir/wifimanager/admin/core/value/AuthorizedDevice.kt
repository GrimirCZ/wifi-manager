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
     * Primary label shown in the ticket device list.
     */
    val displayName: String?,
    /**
     * Device hostname or another device-provided label, if provided.
     */
    val deviceName: String?,
    /**
     * ID of ticket that was used to authorize this device.
     */
    val ticketId: TicketId,
    /**
     * Whether the device's access was revoked by a user.
     */
    val wasAccessRevoked: Boolean,
)
