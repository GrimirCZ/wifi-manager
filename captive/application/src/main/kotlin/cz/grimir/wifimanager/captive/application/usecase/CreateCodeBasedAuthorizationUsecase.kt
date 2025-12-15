package cz.grimir.wifimanager.captive.application.usecase

import cz.grimir.wifimanager.captive.application.command.CreateCodeBasedAuthorizationCommand
import cz.grimir.wifimanager.captive.application.ports.AuthorizationTokenRepository
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import org.springframework.stereotype.Service

@Service
class CreateCodeBasedAuthorizationUsecase(
    val repository: AuthorizationTokenRepository
) {
    fun create(command: CreateCodeBasedAuthorizationCommand) {
        repository.save(
            AuthorizationToken(
                id = command.ticketId,
                accessCode = command.accessCode,
                authorizedDevices = mutableListOf(),
                kickedMacAddresses = mutableSetOf()
            )
        )
    }
}
