package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindUserDevicePort
import cz.grimir.wifimanager.admin.core.value.UserDevice
import org.springframework.stereotype.Service

@Service
class FindAllUserDevicesUsecase(
    private val findUserDevicePort: FindUserDevicePort,
) {
    fun find(): List<UserDevice> = findUserDevicePort.findAll()
}
