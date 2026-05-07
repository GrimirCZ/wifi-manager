package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.shared.events.AuthorizationTokenRemovedEvent
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import cz.grimir.wifimanager.shared.events.DeviceFingerprintMismatchDetectedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceAuthorizedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDevicesLastSeenObservedEvent

interface CaptiveEventPublisher {
    fun publish(event: DeviceAuthorizedEvent)

    fun publish(event: DeviceFingerprintMismatchDetectedEvent)

    fun publish(event: ClientAccessRevokedEvent)

    fun publish(event: AuthorizationTokenRemovedEvent)

    fun publish(event: NetworkUserDeviceAuthorizedEvent)

    fun publish(event: NetworkUserDeviceRemovedEvent)

    fun publish(event: NetworkUserDevicesLastSeenObservedEvent)

    fun publish(event: MacAuthorizationStateChangedEvent)
}
