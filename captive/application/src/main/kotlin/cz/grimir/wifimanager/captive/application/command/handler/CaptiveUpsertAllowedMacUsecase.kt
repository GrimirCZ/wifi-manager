package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.query.model.AllowedMac
import cz.grimir.wifimanager.captive.application.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.port.RouterAgentPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CaptiveUpsertAllowedMacUsecase(
    private val allowedMacWritePort: AllowedMacWritePort,
    private val routerAgentPort: RouterAgentPort,
) {
    @Transactional
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
