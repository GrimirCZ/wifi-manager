package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.shared.core.TicketId

interface FindAuthorizationTokenPort {
    fun findByTicketId(ticketId: TicketId): AuthorizationToken?

    fun findByAccessCode(accessCode: String): AuthorizationToken?

    fun findByAuthorizedDeviceMac(macAddress: String): AuthorizationToken?
}
