package cz.grimir.wifimanager.captive.application.policy

import cz.grimir.wifimanager.captive.application.command.RevokeClientAccessCommand
import cz.grimir.wifimanager.captive.application.command.handler.RevokeClientAccessUsecase
import cz.grimir.wifimanager.shared.application.network.MacAddressNormalizer
import cz.grimir.wifimanager.shared.events.ClientKickedEvent
import org.springframework.stereotype.Service

@Service
class WhenClientKickedRevokeClientAccessPolicy(
    private val revokeClientAccessUsecase: RevokeClientAccessUsecase,
) {
    fun on(event: ClientKickedEvent) {
        revokeClientAccessUsecase.revoke(
            RevokeClientAccessCommand(
                ticketId = event.ticketId,
                deviceMacAddress = MacAddressNormalizer.normalize(event.deviceMacAddress),
            ),
        )
    }
}
