package cz.grimir.wifimanager.admin.application.device.handler.query

import cz.grimir.wifimanager.admin.application.device.port.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.device.query.FindUserDevicesByUserIdQuery
import cz.grimir.wifimanager.admin.core.value.UserDevice
import org.springframework.stereotype.Service

@Service
class FindUserDevicesByUserIdUsecase(
    private val findUserDevicePort: FindUserDevicePort,
) {
    fun find(query: FindUserDevicesByUserIdQuery): List<UserDevice> = findUserDevicePort.findByUserId(query.userId)
}
