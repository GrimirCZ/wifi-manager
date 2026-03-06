package cz.grimir.wifimanager.captive.application.authorization.handler.command

import cz.grimir.wifimanager.captive.application.authorization.command.CreateCodeBasedAuthorizationCommand
import cz.grimir.wifimanager.captive.application.authorization.port.ModifyAuthorizationTokenPort
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
