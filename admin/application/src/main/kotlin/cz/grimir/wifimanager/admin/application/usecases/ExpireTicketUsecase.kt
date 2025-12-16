package cz.grimir.wifimanager.admin.application.usecases

import cz.grimir.wifimanager.admin.application.commands.ExpireTicketCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import org.springframework.stereotype.Service

@Service
class ExpireTicketUsecase(
    private val findTicketPort: FindTicketPort,
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
) {
    fun expire(command: ExpireTicketCommand) {
        // TODO: implement
    }
}
