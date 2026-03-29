package cz.grimir.wifimanager.captive.core.aggregates

import cz.grimir.wifimanager.captive.core.exceptions.KickedAddressAttemptedLoginException
import cz.grimir.wifimanager.captive.core.value.Device
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintProfile
import cz.grimir.wifimanager.captive.core.value.DeviceFingerprintStatus
import cz.grimir.wifimanager.shared.core.TicketId
import java.time.Instant

class AuthorizationToken(
    val id: TicketId,
    /**
     * Access code required to use this ticket.
     */
    val accessCode: String,
    /**
     * UTC time of ticket expiration.
     */
    val validUntil: Instant,
    val requireUserNameOnLogin: Boolean,
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

        val existingIndex = authorizedDevices.indexOfFirst { it.mac == device.mac }
        if (existingIndex >= 0) {
            authorizedDevices[existingIndex] = device
            return
        }

        authorizedDevices.add(device)
    }

    fun updateAuthorizedDevice(device: Device) {
        val existingIndex = authorizedDevices.indexOfFirst { it.mac == device.mac }
        if (existingIndex >= 0) {
            authorizedDevices[existingIndex] = device
            return
        }

        authorizedDevices.add(device)
    }

    fun requireReauthForAuthorizedDevice(
        mac: String,
        at: Instant,
    ): Device {
        val existingIndex = authorizedDevices.indexOfFirst { it.mac == mac }
        require(existingIndex >= 0) { "authorized device not found for mac=$mac" }

        val updated = authorizedDevices[existingIndex].copy(reauthRequiredAt = at)
        authorizedDevices[existingIndex] = updated
        return updated
    }

    fun updateAuthorizedDeviceFingerprint(
        mac: String,
        fingerprintProfile: DeviceFingerprintProfile?,
        fingerprintStatus: DeviceFingerprintStatus,
        fingerprintVerifiedAt: Instant,
    ): Device {
        val existingIndex = authorizedDevices.indexOfFirst { it.mac == mac }
        require(existingIndex >= 0) { "authorized device not found for mac=$mac" }

        val updated =
            authorizedDevices[existingIndex].copy(
                fingerprintProfile = fingerprintProfile,
                fingerprintStatus = fingerprintStatus,
                fingerprintVerifiedAt = fingerprintVerifiedAt,
            )
        authorizedDevices[existingIndex] = updated
        return updated
    }
}
