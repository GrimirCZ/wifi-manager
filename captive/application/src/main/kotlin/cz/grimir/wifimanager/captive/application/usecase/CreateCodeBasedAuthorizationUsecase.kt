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
        // TODO: implement
        modifyAuthorizationTokenPort.save(
            AuthorizationToken(
                id = command.ticketId,
                accessCode = command.accessCode,
                authorizedDevices = mutableListOf(),
                kickedMacAddresses = mutableSetOf(),
            ),
        )
    }
}
