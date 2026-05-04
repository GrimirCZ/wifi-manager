package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.port.SaveUserDevicePort
import cz.grimir.wifimanager.admin.core.value.UserDevice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpsertUserDeviceUsecase(
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    @Transactional
    fun upsert(device: UserDevice) {
        saveUserDevicePort.save(device)
    }
}
