package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.usecases.commands.UpsertUserDeviceUsecase
import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.shared.events.NetworkUserDeviceAuthorizedEvent
import org.springframework.stereotype.Service

@Service
class OnNetworkUserDeviceAuthorizedUpsertUserDevicePolicy(
    private val upsertUserDeviceUsecase: UpsertUserDeviceUsecase,
) {
    fun on(event: NetworkUserDeviceAuthorizedEvent) {
        upsertUserDeviceUsecase.upsert(
            UserDevice(
                userId = event.userId,
                mac = event.deviceMac,
                name = event.deviceName,
                hostname = event.hostname,
                isRandomized = event.isRandomized,
                authorizedAt = event.authorizedAt,
                lastSeenAt = event.lastSeenAt,
            ),
        )
    }
}
