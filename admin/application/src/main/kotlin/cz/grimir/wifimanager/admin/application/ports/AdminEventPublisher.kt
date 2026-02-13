package cz.grimir.wifimanager.admin.application.ports

import cz.grimir.wifimanager.shared.events.AllowedMacRemovedEvent
import cz.grimir.wifimanager.shared.events.AllowedMacUpsertedEvent
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceDeauthorizeRequestedEvent
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import cz.grimir.wifimanager.shared.events.TicketEndedEvent

interface AdminEventPublisher {
    fun publish(event: TicketCreatedEvent)

    fun publish(event: ClientKickedEvent)

    fun publish(event: TicketEndedEvent)

    fun publish(event: AllowedMacUpsertedEvent)

    fun publish(event: AllowedMacRemovedEvent)

    fun publish(event: NetworkUserDeviceDeauthorizeRequestedEvent)
}
