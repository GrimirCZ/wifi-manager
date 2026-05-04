package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.command.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.command.handler.AddAuthorizedDeviceUsecase
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
                displayName = event.device.displayName,
                deviceName = event.device.deviceName,
            ),
        )
    }
}
