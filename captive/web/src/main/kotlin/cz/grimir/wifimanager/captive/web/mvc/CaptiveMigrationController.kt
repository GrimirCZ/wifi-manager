package cz.grimir.wifimanager.captive.web.mvc

import cz.grimir.wifimanager.captive.application.migration.handler.command.ImportMigratedCaptiveUserUsecase
import cz.grimir.wifimanager.captive.application.migration.handler.command.MigrationMacOwnershipConflictException
import cz.grimir.wifimanager.captive.application.migration.handler.command.MigrationUserNotFoundException
import cz.grimir.wifimanager.captive.application.migration.model.ImportMigratedUserCommand
import cz.grimir.wifimanager.captive.application.migration.model.ImportedActiveTicket
import cz.grimir.wifimanager.captive.application.migration.model.ImportedAuthorizedDevice
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveMigrationImportRequest
import cz.grimir.wifimanager.captive.web.mvc.dto.CaptiveMigrationImportResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/captive/api/migration")
class CaptiveMigrationController(
    private val importMigratedCaptiveUserUsecase: ImportMigratedCaptiveUserUsecase,
) {
    @PostMapping("/import-user")
    @ResponseStatus(HttpStatus.OK)
    fun importUser(
        @RequestBody
        request: CaptiveMigrationImportRequest,
    ): CaptiveMigrationImportResponse =
        try {
            validate(request)
            importMigratedCaptiveUserUsecase.importUser(
                ImportMigratedUserCommand(
                    email = request.email,
                    tickets =
                        request.tickets.map { ticket ->
                            ImportedActiveTicket(
                                start = ticket.start ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket start is required."),
                                lengthSeconds = ticket.lengthSeconds,
                                authorizedDevices =
                                    ticket.authorizedDevices.map { device ->
                                        ImportedAuthorizedDevice(
                                            mac = device.mac,
                                            deviceName = device.deviceName,
                                            displayName = device.displayName,
                                        )
                                    },
                            )
                        },
                ),
            ).let { result ->
                CaptiveMigrationImportResponse(
                    userId = result.userId,
                    identityId = result.identityId,
                    email = result.email,
                    importedTicketCount = result.importedTicketCount,
                    importedDeviceCount = result.importedDeviceCount,
                )
            }
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid migration payload.", ex)
        } catch (ex: MigrationUserNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, ex.message, ex)
        } catch (ex: MigrationMacOwnershipConflictException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, ex.message, ex)
        }

    private fun validate(request: CaptiveMigrationImportRequest) {
        val email = request.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.")
        }
        if (request.tickets.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one ticket is required.")
        }
        request.tickets.forEach { ticket ->
            if (ticket.start == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket start is required.")
            }
            if (ticket.lengthSeconds <= 0) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket lengthSeconds must be positive.")
            }
            if (ticket.authorizedDevices.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one authorized device is required.")
            }
            ticket.authorizedDevices.forEach { device ->
                if (device.mac.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Device MAC is required.")
                }
            }
        }
    }
}
