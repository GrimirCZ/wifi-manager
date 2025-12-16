package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken
import cz.grimir.wifimanager.shared.core.TicketId

interface ModifyAuthorizationTokenPort {
    fun save(token: AuthorizationToken)

    fun deleteByTicketId(ticketId: TicketId)
}
