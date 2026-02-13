package cz.grimir.wifimanager.admin.application.usecases.queries

import cz.grimir.wifimanager.admin.application.ports.FindUserDevicePort
import cz.grimir.wifimanager.admin.application.queries.FindUserDevicesByUserIdQuery
import cz.grimir.wifimanager.admin.core.value.UserDevice
import org.springframework.stereotype.Service

@Service
class FindUserDevicesByUserIdUsecase(
    private val findUserDevicePort: FindUserDevicePort,
) {
    fun find(query: FindUserDevicesByUserIdQuery): List<UserDevice> = findUserDevicePort.findByUserId(query.userId)
}
