package cz.grimir.wifimanager.captive.events.local

import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.shared.events.AuthorizationTokenRemovedEvent
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceAuthorizedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDevicesLastSeenObservedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class SpringCaptiveEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CaptiveEventPublisher {
    override fun publish(event: DeviceAuthorizedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: DeviceFingerprintMismatchDetectedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: ClientAccessRevokedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: AuthorizationTokenRemovedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: NetworkUserDeviceAuthorizedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: NetworkUserDeviceRemovedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: NetworkUserDevicesLastSeenObservedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(event: MacAuthorizationStateChangedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
