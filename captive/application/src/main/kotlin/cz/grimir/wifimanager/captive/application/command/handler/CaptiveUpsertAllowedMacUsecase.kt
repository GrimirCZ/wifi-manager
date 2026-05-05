package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.UpsertAllowedMacCommand
import cz.grimir.wifimanager.captive.application.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.port.RouterAgentPort
import cz.grimir.wifimanager.captive.application.query.model.AllowedMac
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CaptiveUpsertAllowedMacUsecase(
    private val allowedMacWritePort: AllowedMacWritePort,
    private val routerAgentPort: RouterAgentPort,
) {
    @Transactional
    fun upsert(command: UpsertAllowedMacCommand) {
        allowedMacWritePort.save(
            AllowedMac(
                mac = command.macAddress,
                validUntil = command.validUntil,
            ),
        )

        routerAgentPort.allowClientAccess(listOf(command.macAddress))
    }
}
