package cz.grimir.wifimanager.captive.application.allowedmac.handler.command

import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.authorization.port.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import org.springframework.stereotype.Service

@Service
class CaptiveRemoveAllowedMacUsecase(
    private val allowedMacReadPort: AllowedMacReadPort,
    private val allowedMacWritePort: AllowedMacWritePort,
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val routerAgentPort: RouterAgentPort,
) {
    fun remove(macAddress: String) {
        val existing = allowedMacReadPort.findByMac(macAddress) ?: return
        allowedMacWritePort.deleteByMac(existing.mac)

        val token = findAuthorizationTokenPort.findByAuthorizedDeviceMac(macAddress)
        val isAuthorized = token != null && !token.kickedMacAddresses.contains(macAddress)
        if (!isAuthorized) {
            routerAgentPort.revokeClientAccess(listOf(macAddress))
        }
    }
}
