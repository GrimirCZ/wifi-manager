package cz.grimir.wifimanager.captive.application.allowedmac.handler.command

import cz.grimir.wifimanager.captive.application.allowedmac.model.AllowedMac
import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.integration.routeragent.port.RouterAgentPort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CaptiveUpsertAllowedMacUsecase(
    private val allowedMacWritePort: AllowedMacWritePort,
    private val routerAgentPort: RouterAgentPort,
) {
    fun upsert(
        macAddress: String,
        validUntil: Instant?,
    ) {
        allowedMacWritePort.save(
            AllowedMac(
                mac = macAddress,
                validUntil = validUntil,
            ),
        )

        routerAgentPort.allowClientAccess(listOf(macAddress))
    }
}
