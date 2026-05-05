package cz.grimir.wifimanager.admin.application.query.handler

import cz.grimir.wifimanager.admin.application.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.query.FindAllUserDevicesQuery
import cz.grimir.wifimanager.admin.core.value.UserDevice
import org.springframework.stereotype.Service

@Service
class FindAllUserDevicesUsecase(
    private val findUserDevicePort: FindUserDevicePort,
) {
    fun find(query: FindAllUserDevicesQuery): List<UserDevice> = findUserDevicePort.findAll()
}
