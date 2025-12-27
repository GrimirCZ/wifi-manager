package cz.grimir.wifimanager.captive.application.usecase

import cz.grimir.wifimanager.captive.application.command.CreateCodeBasedAuthorizationCommand
import cz.grimir.wifimanager.captive.application.ports.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import org.springframework.stereotype.Service

@Service
class CreateCodeBasedAuthorizationUsecase(
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
) {
    fun create(command: CreateCodeBasedAuthorizationCommand) {
        modifyAuthorizationTokenPort.save(
            AuthorizationToken(
                id = command.ticketId,
                accessCode = command.accessCode,
                validUntil = command.validUntil,
                authorizedDevices = mutableListOf(),
                kickedMacAddresses = mutableSetOf(),
            ),
        )
    }
}
