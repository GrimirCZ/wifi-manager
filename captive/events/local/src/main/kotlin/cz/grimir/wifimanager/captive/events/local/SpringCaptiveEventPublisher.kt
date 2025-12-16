package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.ports.CaptiveEventPublisher
import cz.grimir.wifimanager.shared.events.AuthorizationTokenRemovedEvent
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class SpringCaptiveEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CaptiveEventPublisher {
    override fun publish(event: DeviceAuthorizedEvent) {
        // TODO: implement (transport concerns, tracing, etc.)
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: ClientAccessRevokedEvent) {
        // TODO: implement (transport concerns, tracing, etc.)
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: AuthorizationTokenRemovedEvent) {
        // TODO: implement (transport concerns, tracing, etc.)
        applicationEventPublisher.publishEvent(event)
    }
}
