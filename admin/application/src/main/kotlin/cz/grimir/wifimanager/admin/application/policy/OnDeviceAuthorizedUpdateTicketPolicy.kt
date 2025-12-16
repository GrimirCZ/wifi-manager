package cz.grimir.wifimanager.admin.application.policy

import cz.grimir.wifimanager.admin.application.commands.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.usecases.AddAuthorizedDeviceUsecase
import cz.grimir.wifimanager.shared.events.DeviceAuthorizedEvent
import org.springframework.stereotype.Service

@Service
class OnDeviceAuthorizedUpdateTicketPolicy(
    private val addAuthorizedDeviceUsecase: AddAuthorizedDeviceUsecase,
) {
    fun on(event: DeviceAuthorizedEvent) {
        // TODO: implement
        addAuthorizedDeviceUsecase.add(
            AddAuthorizedDeviceCommand(
                ticketId = event.ticketId,
                deviceMacAddress = event.device.macAddress,
                deviceName = event.device.name,
            )
        )
    }
}

