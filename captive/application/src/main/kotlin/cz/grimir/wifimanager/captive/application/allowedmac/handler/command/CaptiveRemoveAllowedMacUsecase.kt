package cz.grimir.wifimanager.captive.application.allowedmac.handler.command

import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacReadPort
import cz.grimir.wifimanager.captive.application.allowedmac.port.AllowedMacWritePort
import cz.grimir.wifimanager.captive.application.authorization.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CaptiveRemoveAllowedMacUsecase(
    private val allowedMacReadPort: AllowedMacReadPort,
    private val allowedMacWritePort: AllowedMacWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
) {
    @Transactional
    fun remove(macAddress: String) {
        val existing = allowedMacReadPort.findByMac(macAddress) ?: return
        allowedMacWritePort.deleteByMac(existing.mac)
        captiveEventPublisher.publish(MacAuthorizationStateChangedEvent(listOf(existing.mac)))
    }
}
