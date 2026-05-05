package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.RemoveNetworkUserDeviceCommand
import cz.grimir.wifimanager.captive.application.event.MacAuthorizationStateChangedEvent
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class RemoveNetworkUserDeviceUsecase(
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
    private val timeProvider: TimeProvider,
) {
    @Transactional
    fun remove(command: RemoveNetworkUserDeviceCommand) {
        networkUserDeviceWritePort.delete(command.userId, command.mac)
        logger.info { "Network user device removed userId=${command.userId} mac=${command.mac}" }
        captiveEventPublisher.publish(
            NetworkUserDeviceRemovedEvent(
                userId = command.userId,
                deviceMac = command.mac,
                removedAt = timeProvider.get(),
            ),
        )
        captiveEventPublisher.publish(MacAuthorizationStateChangedEvent(listOf(command.mac)))
    }
}
