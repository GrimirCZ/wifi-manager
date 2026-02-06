package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.usecase.commands.CaptiveRemoveAllowedMacUsecase
import cz.grimir.wifimanager.captive.application.usecase.commands.CaptiveUpsertAllowedMacUsecase
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
        upsertAllowedMacUsecase.upsert(event.macAddress, event.validUntil)
    }

    @EventListener
    fun on(event: AllowedMacRemovedEvent) {
        removeAllowedMacUsecase.remove(event.macAddress)
    }
}
