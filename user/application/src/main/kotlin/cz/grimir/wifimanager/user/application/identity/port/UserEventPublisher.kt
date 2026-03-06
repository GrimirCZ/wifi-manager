package cz.grimir.wifimanager.user.application.identity.port

import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent

interface UserEventPublisher {
    fun publish(event: UserCreatedOrUpdatedEvent)
}
