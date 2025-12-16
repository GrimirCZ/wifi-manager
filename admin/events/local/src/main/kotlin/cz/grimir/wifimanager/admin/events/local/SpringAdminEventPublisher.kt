package cz.grimir.wifimanager.admin.events.local

import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import cz.grimir.wifimanager.shared.events.TicketEndedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class SpringAdminEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : AdminEventPublisher {
    override fun publish(event: TicketCreatedEvent) {
        // TODO: implement (transport concerns, tracing, etc.)
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: ClientKickedEvent) {
        // TODO: implement (transport concerns, tracing, etc.)
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: TicketEndedEvent) {
        // TODO: implement (transport concerns, tracing, etc.)
        applicationEventPublisher.publishEvent(event)
    }
}

