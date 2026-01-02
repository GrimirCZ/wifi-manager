package cz.grimir.wifimanager.user.events.local

import cz.grimir.wifimanager.shared.events.UserCreatedOrUpdatedEvent
import cz.grimir.wifimanager.user.application.ports.UserEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class SpringUserEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : UserEventPublisher {
    override fun publish(event: UserCreatedOrUpdatedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
