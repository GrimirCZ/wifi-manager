package cz.grimir.wifimanager.admin.application.command.handler

import cz.grimir.wifimanager.admin.application.command.RemoveUserDeviceCommand
import cz.grimir.wifimanager.admin.application.port.DeleteUserDevicePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveUserDeviceUsecase(
    private val deleteUserDevicePort: DeleteUserDevicePort,
) {
    @Transactional
    fun remove(command: RemoveUserDeviceCommand) {
        deleteUserDevicePort.delete(command.userId, command.mac)
    }
}
