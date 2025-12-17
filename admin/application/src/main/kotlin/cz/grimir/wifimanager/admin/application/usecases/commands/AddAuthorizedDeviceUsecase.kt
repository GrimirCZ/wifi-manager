package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.AddAuthorizedDeviceCommand
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import org.springframework.stereotype.Service

@Service
class AddAuthorizedDeviceUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
) {
    fun add(command: AddAuthorizedDeviceCommand) {
        // TODO: implement
    }
}
