package cz.grimir.wifimanager.captive.application.command.handler

import cz.grimir.wifimanager.captive.application.command.CreateCodeBasedAuthorizationCommand
import cz.grimir.wifimanager.captive.application.port.ModifyAuthorizationTokenPort
import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateCodeBasedAuthorizationUsecase(
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
) {
    @Transactional
    fun create(command: CreateCodeBasedAuthorizationCommand) {
        modifyAuthorizationTokenPort.save(
            AuthorizationToken(
                id = command.ticketId,
                accessCode = command.accessCode,
                validUntil = command.validUntil,
                requireUserNameOnLogin = command.requireUserNameOnLogin,
                authorizedDevices = mutableListOf(),
                kickedMacAddresses = mutableSetOf(),
            ),
        )
    }
}
