package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import cz.grimir.wifimanager.shared.events.TicketEndedEvent

interface AdminEventPublisher {
    fun publish(event: TicketCreatedEvent)
    fun publish(event: ClientKickedEvent)
    fun publish(event: TicketEndedEvent)
}

