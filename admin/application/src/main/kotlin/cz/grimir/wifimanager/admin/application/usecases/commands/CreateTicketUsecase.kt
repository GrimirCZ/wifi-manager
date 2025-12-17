package cz.grimir.wifimanager.admin.application.usecases.commands

import cz.grimir.wifimanager.admin.application.commands.CreateTicketCommand
import cz.grimir.wifimanager.admin.application.ports.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ports.SaveTicketPort
import org.springframework.stereotype.Service

@Service
class CreateTicketUsecase(
    private val saveTicketPort: SaveTicketPort,
    private val eventPublisher: AdminEventPublisher,
) {
    fun create(command: CreateTicketCommand) {
        // TODO: implement
    }
}
