package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.persistence.projection.CaptiveUserReadModelUpdater
import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CaptiveUserCreatedOrUpdatedEventListener(
    private val updater: CaptiveUserReadModelUpdater,
) {
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun on(event: UserCreatedOrUpdatedEvent) {
        updater.apply(event)
    }
}
