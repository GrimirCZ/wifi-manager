package cz.grimir.wifimanager.captive.application.usecase.commands

import cz.grimir.wifimanager.captive.application.network.NetworkUserDevice
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceReadPort
import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.TimeProvider
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class AuthorizeNetworkUserDeviceUsecase(
    private val networkUserDeviceReadPort: NetworkUserDeviceReadPort,
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
    private val timeProvider: TimeProvider,
) {
    fun authorize(
        userId: UserId,
        mac: String,
        name: String?,
        hostname: String?,
        isRandomized: Boolean,
    ) {
        val now = timeProvider.get()
        val existing = networkUserDeviceReadPort.findByMac(mac)
        if (existing != null && existing.userId != userId) {
            throw DeviceOwnershipException("Device $mac is already authorized for another user.")
        }
        val device =
            if (existing == null) {
                NetworkUserDevice(
                    userId = userId,
                    mac = mac,
                    name = name,
                    hostname = hostname,
                    isRandomized = isRandomized,
                    authorizedAt = now,
                    lastSeenAt = now,
                )
            } else {
                existing.copy(
                    userId = userId,
                    name = name,
                    hostname = hostname,
                    isRandomized = isRandomized,
                    lastSeenAt = now,
                )
            }
        networkUserDeviceWritePort.save(device)
    }
}
