package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.persistence.projection.AdminUserReadModelUpdater
import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class AdminUserCreatedOrUpdatedEventListener(
    private val updater: AdminUserReadModelUpdater,
) {
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun on(event: UserCreatedOrUpdatedEvent) {
        updater.apply(event)
    }
}
