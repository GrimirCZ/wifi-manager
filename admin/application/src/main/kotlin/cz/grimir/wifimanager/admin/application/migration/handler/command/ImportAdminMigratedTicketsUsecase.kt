package cz.grimir.wifimanager.admin.application.migration.handler.command

import cz.grimir.wifimanager.admin.application.migration.command.ImportAdminMigratedTicketsCommand
import cz.grimir.wifimanager.admin.application.migration.model.ImportedAdminTicketResult
import cz.grimir.wifimanager.admin.application.shared.port.AdminEventPublisher
import cz.grimir.wifimanager.admin.application.ticket.port.SaveAuthorizedDevicePort
import cz.grimir.wifimanager.admin.application.ticket.port.SaveTicketPort
import cz.grimir.wifimanager.admin.application.ticket.support.AccessCodeGenerator
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.value.AuthorizedDevice
import cz.grimir.wifimanager.shared.core.TicketId
import cz.grimir.wifimanager.shared.events.TicketCreatedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImportAdminMigratedTicketsUsecase(
    private val saveTicketPort: SaveTicketPort,
    private val saveAuthorizedDevicePort: SaveAuthorizedDevicePort,
    private val eventPublisher: AdminEventPublisher,
    private val accessCodeGenerator: AccessCodeGenerator,
) {
    @Transactional
    fun import(command: ImportAdminMigratedTicketsCommand): List<ImportedAdminTicketResult> =
        command.tickets.map { importedTicket ->
            val ticketId = TicketId.new()
            val accessCode = accessCodeGenerator.generate(8)
            val validUntil = importedTicket.start.plusSeconds(importedTicket.lengthSeconds)

            val ticket =
                Ticket(
                    id = ticketId,
                    accessCode = accessCode,
                    createdAt = importedTicket.start,
                    validUntil = validUntil,
                    wasCanceled = false,
                    authorId = command.user.userId,
                    requireUserNameOnLogin = importedTicket.requireUserNameOnLogin,
                    kickedMacAddresses = mutableSetOf(),
                )

            saveTicketPort.save(ticket)

            importedTicket.authorizedDevices.forEach { device ->
                saveAuthorizedDevicePort.save(
                    AuthorizedDevice(
                        mac = device.mac,
                        displayName = device.displayName ?: device.deviceName,
                        deviceName = device.deviceName,
                        ticketId = ticketId,
                        wasAccessRevoked = false,
                    ),
                )
            }

            eventPublisher.publish(
                TicketCreatedEvent(
                    id = ticket.id,
                    accessCode = ticket.accessCode,
                    createdAt = ticket.createdAt,
                    validUntil = ticket.validUntil,
                    requireUserNameOnLogin = ticket.requireUserNameOnLogin,
                    author =
                        TicketCreatedEvent.Author(
                            userId = command.user.userId,
                            email = command.user.email,
                            displayName = command.user.displayName,
                        ),
                ),
            )

            ImportedAdminTicketResult(
                ticketId = ticket.id,
                accessCode = ticket.accessCode,
                createdAt = ticket.createdAt,
                validUntil = ticket.validUntil,
            )
        }
}
