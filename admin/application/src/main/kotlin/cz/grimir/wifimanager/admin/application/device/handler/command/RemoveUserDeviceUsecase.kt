package cz.grimir.wifimanager.admin.application.device.handler.command

import cz.grimir.wifimanager.admin.application.device.port.DeleteUserDevicePort
import cz.grimir.wifimanager.shared.core.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveUserDeviceUsecase(
    private val deleteUserDevicePort: DeleteUserDevicePort,
) {
    @Transactional
    fun remove(
        userId: UserId,
        mac: String,
    ) {
        deleteUserDevicePort.delete(userId, mac)
    }
}
