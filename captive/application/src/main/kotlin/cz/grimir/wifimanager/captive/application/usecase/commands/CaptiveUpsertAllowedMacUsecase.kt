package cz.grimir.wifimanager.captive.application.usecase.commands

import cz.grimir.wifimanager.captive.application.allowed.AllowedMac
import cz.grimir.wifimanager.captive.application.ports.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
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
