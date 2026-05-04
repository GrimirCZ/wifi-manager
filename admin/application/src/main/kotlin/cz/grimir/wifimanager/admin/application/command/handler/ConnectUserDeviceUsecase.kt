package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.port.SaveUserDevicePort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ConnectUserDeviceUsecase(
    private val findUserDevicePort: FindUserDevicePort,
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    @Transactional
    fun connect(
        userId: UserId,
        mac: String,
        connectedAt: Instant,
    ) {
        val existing = findUserDevicePort.findByUserIdAndMac(userId, mac) ?: return
        saveUserDevicePort.save(existing.copy(lastSeenAt = connectedAt))
    }
}
