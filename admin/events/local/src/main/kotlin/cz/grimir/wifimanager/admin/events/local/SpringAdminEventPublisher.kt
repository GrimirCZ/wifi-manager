package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import cz.grimir.wifimanager.shared.events.AllowedMacUpsertedEvent
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class SpringAdminEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : AdminEventPublisher {
    override fun publish(event: TicketCreatedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: ClientKickedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: TicketEndedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: AllowedMacUpsertedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: AllowedMacRemovedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: NetworkUserDeviceDeauthorizeRequestedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
