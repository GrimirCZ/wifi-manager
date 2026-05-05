package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.TouchNetworkUserDeviceCommand
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceConnectedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TouchNetworkUserDeviceUsecase(
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun touch(command: TouchNetworkUserDeviceCommand) {
        networkUserDeviceWritePort.touchDevice(command.userId, command.mac)
        captiveEventPublisher.publish(
            NetworkUserDeviceConnectedEvent(
                userId = command.userId,
                deviceMac = command.mac,
                connectedAt = timeProvider.get(),
            ),
        )
    }
}
