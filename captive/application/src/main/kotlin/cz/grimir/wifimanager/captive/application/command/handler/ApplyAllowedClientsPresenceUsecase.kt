package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.ApplyAllowedClientsPresenceCommand
import cz.grimir.wifimanager.captive.application.port.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.port.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceLastSeenChange
import cz.grimir.wifimanager.shared.events.NetworkUserDevicesLastSeenObservedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplyAllowedClientsPresenceUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val captiveEventPublisher: CaptiveEventPublisher,
) {
    @Transactional
    fun apply(command: ApplyAllowedClientsPresenceCommand) {
        if (command.entries.isEmpty()) {
            return
        }
        val latestByMac =
            command.entries
                .map { MacAddressNormalizer.normalize(it.macAddress) to it.lastSeenAt }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, instants) -> instants.max() }

        val changes = mutableListOf<NetworkUserDeviceLastSeenChange>()
        for ((mac, reportedAt) in latestByMac) {
            val device = networkUserDeviceReadPort.findByMac(mac) ?: continue
            if (reportedAt <= device.lastSeenAt) {
                continue
            }
            val updated =
                networkUserDeviceWritePort.updateLastSeenAtIfNewer(
                    userId = device.userId,
                    mac = mac,
                    lastSeenAt = reportedAt,
                )
            if (updated) {
                changes.add(
                    NetworkUserDeviceLastSeenChange(
                        userId = device.userId,
                        deviceMac = mac,
                        lastSeenAt = reportedAt,
                    ),
                )
            }
        }
        if (changes.isNotEmpty()) {
            captiveEventPublisher.publish(NetworkUserDevicesLastSeenObservedEvent(changes))
        }
    }
}
