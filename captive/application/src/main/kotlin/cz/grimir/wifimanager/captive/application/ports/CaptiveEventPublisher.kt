package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.shared.events.AuthorizationTokenRemovedEvent
import cz.grimir.wifimanager.shared.events.ClientAccessRevokedEvent
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceAuthorizedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceConnectedEvent
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent

interface CaptiveEventPublisher {
    fun publish(event: DeviceAuthorizedEvent)

    fun publish(event: ClientAccessRevokedEvent)

    fun publish(event: AuthorizationTokenRemovedEvent)

    fun publish(event: NetworkUserDeviceAuthorizedEvent)

    fun publish(event: NetworkUserDeviceRemovedEvent)

    fun publish(event: NetworkUserDeviceConnectedEvent)
}
