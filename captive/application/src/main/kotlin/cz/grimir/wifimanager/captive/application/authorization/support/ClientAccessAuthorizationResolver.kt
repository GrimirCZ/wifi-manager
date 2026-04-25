package cz.grimir.wifimanager.captive.application.authorization.support

import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceReadPort
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
}
