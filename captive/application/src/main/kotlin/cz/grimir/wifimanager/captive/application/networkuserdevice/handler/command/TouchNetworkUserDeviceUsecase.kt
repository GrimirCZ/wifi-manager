package cz.grimir.wifimanager.captive.application.networkuserdevice.handler.command

import cz.grimir.wifimanager.captive.application.shared.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.networkuserdevice.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceConnectedEvent
import org.springframework.stereotype.Service

@Service
class TouchNetworkUserDeviceUsecase(
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    fun touch(
        userId: UserId,
        mac: String,
    ) {
        networkUserDeviceWritePort.touchDevice(userId, mac)
        captiveEventPublisher.publish(
            NetworkUserDeviceConnectedEvent(
                userId = userId,
                deviceMac = mac,
                connectedAt = timeProvider.get(),
            ),
        )
    }
}
