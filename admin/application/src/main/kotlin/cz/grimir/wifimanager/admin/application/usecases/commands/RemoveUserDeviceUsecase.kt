package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.ports.DeleteUserDevicePort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service

@Service
class RemoveUserDeviceUsecase(
    private val deleteUserDevicePort: DeleteUserDevicePort,
) {
    fun remove(
        userId: UserId,
        mac: String,
    ) {
        deleteUserDevicePort.delete(userId, mac)
    }
}
