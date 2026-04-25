package cz.grimir.wifimanager.captive.application.authorization.port

import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.shared.core.TicketId

interface FindAuthorizationTokenPort {
    fun findByTicketId(ticketId: TicketId): AuthorizationToken?

    fun findByAccessCode(accessCode: String): AuthorizationToken?

    fun findByAuthorizedDeviceMac(macAddress: String): AuthorizationToken?

    fun findAllAuthorizedMacs(): List<String>
}
