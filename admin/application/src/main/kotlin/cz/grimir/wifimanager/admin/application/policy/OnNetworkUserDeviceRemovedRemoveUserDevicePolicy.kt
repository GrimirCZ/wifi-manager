package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.usecases.commands.RemoveUserDeviceUsecase
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import org.springframework.stereotype.Service

@Service
class OnNetworkUserDeviceRemovedRemoveUserDevicePolicy(
    private val removeUserDeviceUsecase: RemoveUserDeviceUsecase,
) {
    fun on(event: NetworkUserDeviceRemovedEvent) {
        removeUserDeviceUsecase.remove(
            userId = event.userId,
            mac = event.deviceMac,
        )
    }
}
