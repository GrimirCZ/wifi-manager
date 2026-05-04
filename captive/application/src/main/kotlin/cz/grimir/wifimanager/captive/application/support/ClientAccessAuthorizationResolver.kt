package cz.grimir.wifimanager.captive.application.support

import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.shared.core.TimeProvider
import org.springframework.stereotype.Service

@Service
class ClientAccessAuthorizationResolver(
    private val allowedMacReadPort: AllowedMacReadPort,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val timeProvider: TimeProvider,
) {
    fun isAuthorizedByAllowedMac(macAddress: String): Boolean {
        val allowedMac = allowedMacReadPort.findByMac(macAddress) ?: return false
        return allowedMac.validUntil?.isAfter(timeProvider.get()) ?: true
    }

    fun isAuthorizedByActiveTicketDevice(macAddress: String): Boolean {
        val token = findAuthorizationTokenPort.findByAuthorizedDeviceMac(macAddress) ?: return false
        val device = token.authorizedDevices.firstOrNull { it.mac == macAddress } ?: return false
        return !token.kickedMacAddresses.contains(macAddress) && device.reauthRequiredAt == null
    }

    fun isAuthorizedByAccountDevice(macAddress: String): Boolean {
        val device = networkUserDeviceReadPort.findByMac(macAddress) ?: return false
        return device.reauthRequiredAt == null
    }

    fun isDevicePrivacyCleanupEligible(macAddress: String): Boolean =
        !hasCurrentAllowedMac(macAddress) &&
            !hasTicketDeviceThatCanReauthorize(macAddress) &&
            !hasAccountDevice(macAddress)

    private fun hasCurrentAllowedMac(macAddress: String): Boolean {
        val allowedMac = allowedMacReadPort.findByMac(macAddress) ?: return false
        return allowedMac.validUntil?.isAfter(timeProvider.get()) ?: true
    }

    private fun hasTicketDeviceThatCanReauthorize(macAddress: String): Boolean {
        val token = findAuthorizationTokenPort.findByAuthorizedDeviceMac(macAddress) ?: return false
        return !token.kickedMacAddresses.contains(macAddress) &&
            token.authorizedDevices.any { it.mac == macAddress }
    }

    private fun hasAccountDevice(macAddress: String): Boolean = networkUserDeviceReadPort.findByMac(macAddress) != null
}
