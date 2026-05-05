package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.command.ConnectUserDeviceCommand
import cz.grimir.wifimanager.admin.application.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.port.SaveUserDevicePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConnectUserDeviceUsecase(
    private val findUserDevicePort: FindUserDevicePort,
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    @Transactional
    fun connect(command: ConnectUserDeviceCommand) {
        val existing = findUserDevicePort.findByUserIdAndMac(command.userId, command.mac) ?: return
        saveUserDevicePort.save(existing.copy(lastSeenAt = command.connectedAt))
    }
}
