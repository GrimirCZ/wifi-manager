package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.command.RemoveUserDeviceCommand
import cz.grimir.wifimanager.admin.application.command.handler.RemoveUserDeviceUsecase
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceRemovedEvent
import org.springframework.stereotype.Service

@Service
class OnNetworkUserDeviceRemovedRemoveUserDevicePolicy(
    private val removeUserDeviceUsecase: RemoveUserDeviceUsecase,
) {
    fun on(event: NetworkUserDeviceRemovedEvent) {
        removeUserDeviceUsecase.remove(
            RemoveUserDeviceCommand(
                userId = event.userId,
                mac = event.deviceMac,
            ),
        )
    }
}
