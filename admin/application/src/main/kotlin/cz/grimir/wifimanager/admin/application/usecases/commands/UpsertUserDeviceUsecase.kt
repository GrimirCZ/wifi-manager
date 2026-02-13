package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.ports.SaveUserDevicePort
import cz.grimir.wifimanager.admin.core.value.UserDevice
import org.springframework.stereotype.Service

@Service
class UpsertUserDeviceUsecase(
    private val saveUserDevicePort: SaveUserDevicePort,
) {
    fun upsert(device: UserDevice) {
        saveUserDevicePort.save(device)
    }
}
