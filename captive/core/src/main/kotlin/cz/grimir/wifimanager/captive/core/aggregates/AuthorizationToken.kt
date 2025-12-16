package cz.grimir.wifimanager.captive.core.aggregates

import cz.grimir.wifimanager.captive.core.exceptions.KickedAddressAttemptedLoginException
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.shared.core.TicketId

class AuthorizationToken(
    val id: TicketId,
    /**
     * Access code required to use this ticket.
     */
    val accessCode: String,
    /**
     * List of devices authorized using this ticket.
     */
    var authorizedDevices: MutableList<Device>,
    /**
     * List of MAC addresses whose access was revoked by the user.
     */
    var kickedMacAddresses: MutableSet<String>,
) {
    fun authorizeDevice(device: Device) {
        if (kickedMacAddresses.contains(device.mac)) {
            throw KickedAddressAttemptedLoginException(device.mac)
        }

        authorizedDevices.add(device)
    }
}
