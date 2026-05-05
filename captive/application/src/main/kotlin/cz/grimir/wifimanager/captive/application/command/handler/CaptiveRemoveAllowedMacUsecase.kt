package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.RemoveAllowedMacCommand
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CaptiveRemoveAllowedMacUsecase(
    private val allowedMacReadPort: AllowedMacReadPort,
    private val allowedMacWritePort: AllowedMacWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
) {
    @Transactional
    fun remove(command: RemoveAllowedMacCommand) {
        val existing = allowedMacReadPort.findByMac(command.macAddress) ?: return
        allowedMacWritePort.deleteByMac(existing.mac)
        captiveEventPublisher.publish(MacAuthorizationStateChangedEvent(listOf(existing.mac)))
    }
}
