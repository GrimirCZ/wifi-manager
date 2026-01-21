package cz.grimir.wifimanager.captive.application.usecase.commands

import cz.grimir.wifimanager.captive.application.ports.NetworkUserDeviceWritePort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class RemoveNetworkUserDeviceUsecase(
    private val networkUserDeviceWritePort: NetworkUserDeviceWritePort,
) {
    fun remove(
        userId: UserId,
        mac: String,
    ) {
        networkUserDeviceWritePort.delete(userId, mac)
    }
}
