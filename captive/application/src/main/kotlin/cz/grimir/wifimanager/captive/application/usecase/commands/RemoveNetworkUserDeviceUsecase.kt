package cz.grimir.wifimanager.captive.application.usecase.commands

import cz.grimir.wifimanager.captive.application.ports.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveNetworkUserDeviceUsecase(
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun remove(
        userId: UserId,
        mac: String,
    ) {
        networkUserDeviceWritePort.delete(userId, mac)
        captiveEventPublisher.publish(
            NetworkUserDeviceRemovedEvent(
                userId = userId,
                deviceMac = mac,
                removedAt = timeProvider.get(),
            ),
        )
    }
}
