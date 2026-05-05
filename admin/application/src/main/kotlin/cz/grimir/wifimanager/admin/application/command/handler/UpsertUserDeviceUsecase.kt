package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.command.UpsertUserDeviceCommand
import cz.grimir.wifimanager.admin.application.port.SaveUserDevicePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpsertUserDeviceUsecase(
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    @Transactional
    fun upsert(command: UpsertUserDeviceCommand) {
        saveUserDevicePort.save(command.device)
    }
}
