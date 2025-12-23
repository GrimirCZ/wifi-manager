package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.commands.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.usecases.commands.AddAuthorizedDeviceUsecase
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.stereotype.Service

@Service
class OnDeviceAuthorizedCreateAuthorizedDevicePolicy(
    private val usecase: AddAuthorizedDeviceUsecase,
) {
    fun on(event: DeviceAuthorizedEvent) {
        usecase.add(
            AddAuthorizedDeviceCommand(
                ticketId = event.ticketId,
                deviceMacAddress = event.device.macAddress,
                deviceName = event.device.name,
            ),
        )
    }
}
