package cz.grimir.wifimanager.user.application.ports

import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent

interface UserEventPublisher {
    fun publish(event: UserCreatedOrUpdatedEvent)
}
