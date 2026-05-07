package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.command.UpdateUserDevicesLastSeenCommand
import cz.grimir.wifimanager.admin.application.command.handler.UpdateUserDevicesLastSeenUsecase
import cz.grimir.wifimanager.shared.events.NetworkUserDevicesLastSeenObservedEvent
import org.springframework.stereotype.Service

@Service
class OnNetworkUserDevicesLastSeenObservedPolicy(
    private val updateUserDevicesLastSeenUsecase: UpdateUserDevicesLastSeenUsecase,
) {
    fun on(event: NetworkUserDevicesLastSeenObservedEvent) {
        if (event.changes.isEmpty()) {
            return
        }
        updateUserDevicesLastSeenUsecase.update(UpdateUserDevicesLastSeenCommand(event.changes))
    }
}
