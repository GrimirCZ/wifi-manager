package cz.grimir.wifimanager.captive.application.usecase

import cz.grimir.wifimanager.captive.application.command.RevokeClientAccessCommand
import cz.grimir.wifimanager.captive.application.ports.CaptiveEventPublisher
import cz.grimir.wifimanager.captive.application.ports.FindAuthorizationTokenPort
import cz.grimir.wifimanager.captive.application.ports.RouterAgentPort
import cz.grimir.wifimanager.captive.application.ports.ModifyAuthorizationTokenPort
import org.springframework.stereotype.Service

@Service
class RevokeClientAccessUsecase(
    private val findAuthorizationTokenPort: FindAuthorizationTokenPort,
    private val modifyAuthorizationTokenPort: ModifyAuthorizationTokenPort,
    private val routerAgentPort: RouterAgentPort,
    private val eventPublisher: CaptiveEventPublisher,
) {
    fun revoke(command: RevokeClientAccessCommand) {
        // TODO: implement
    }
}
