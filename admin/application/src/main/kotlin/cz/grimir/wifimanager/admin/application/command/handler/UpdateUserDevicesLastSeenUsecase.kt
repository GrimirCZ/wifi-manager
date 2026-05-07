package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.command.UpdateUserDevicesLastSeenCommand
import cz.grimir.wifimanager.admin.application.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.port.SaveUserDevicePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateUserDevicesLastSeenUsecase(
    private val findUserDevicePort: FindUserDevicePort,
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    @Transactional
    fun update(command: UpdateUserDevicesLastSeenCommand) {
        for (change in command.changes) {
            val existing = findUserDevicePort.findByUserIdAndMac(change.userId, change.deviceMac) ?: continue
            if (change.lastSeenAt <= existing.lastSeenAt) {
                continue
            }
            saveUserDevicePort.save(existing.copy(lastSeenAt = change.lastSeenAt))
        }
    }
}
