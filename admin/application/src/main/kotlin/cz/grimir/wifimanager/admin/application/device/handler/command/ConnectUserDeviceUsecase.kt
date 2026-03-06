package cz.grimir.wifimanager.admin.application.device.handler.command

import cz.grimir.wifimanager.admin.application.device.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.device.port.SaveUserDevicePort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ConnectUserDeviceUsecase(
    private val findUserDevicePort: FindUserDevicePort,
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    fun connect(
        userId: UserId,
        mac: String,
        connectedAt: Instant,
    ) {
        val existing = findUserDevicePort.findByUserIdAndMac(userId, mac) ?: return
        saveUserDevicePort.save(existing.copy(lastSeenAt = connectedAt))
    }
}
