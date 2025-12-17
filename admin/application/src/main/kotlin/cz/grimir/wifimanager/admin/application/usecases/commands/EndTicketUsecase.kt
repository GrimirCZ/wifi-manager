package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.EndTicketCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import org.springframework.stereotype.Service

@Service
class EndTicketUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
) {
    fun end(command: EndTicketCommand) {
        // TODO: implement
    }
}
