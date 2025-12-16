package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.shared.events.AuthorizationTokenRemovedEvent
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent

interface CaptiveEventPublisher {
    fun publish(event: DeviceAuthorizedEvent)
    fun publish(event: ClientAccessRevokedEvent)
    fun publish(event: AuthorizationTokenRemovedEvent)
}

