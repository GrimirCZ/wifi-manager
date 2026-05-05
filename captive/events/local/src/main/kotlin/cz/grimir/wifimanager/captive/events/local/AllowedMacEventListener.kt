package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.command.RemoveAllowedMacCommand
import cz.grimir.wifimanager.captive.application.command.UpsertAllowedMacCommand
import cz.grimir.wifimanager.captive.application.command.handler.CaptiveRemoveAllowedMacUsecase
import cz.grimir.wifimanager.captive.application.command.handler.CaptiveUpsertAllowedMacUsecase
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import cz.grimir.wifimanager.shared.events.AllowedMacUpsertedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AllowedMacEventListener(
    private val upsertAllowedMacUsecase: CaptiveUpsertAllowedMacUsecase,
    private val removeAllowedMacUsecase: CaptiveRemoveAllowedMacUsecase,
) {
    @EventListener
    fun on(event: AllowedMacUpsertedEvent) {
        upsertAllowedMacUsecase.upsert(
            UpsertAllowedMacCommand(
                macAddress = MacAddressNormalizer.normalize(event.macAddress),
                validUntil = event.validUntil,
            ),
        )
    }

    @EventListener
    fun on(event: AllowedMacRemovedEvent) {
        removeAllowedMacUsecase.remove(RemoveAllowedMacCommand(MacAddressNormalizer.normalize(event.macAddress)))
    }
}
