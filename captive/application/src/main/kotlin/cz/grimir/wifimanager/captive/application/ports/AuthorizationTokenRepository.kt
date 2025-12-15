package cz.grimir.wifimanager.captive.application.ports

import cz.grimir.wifimanager.captive.core.aggregates.AuthorizationToken

interface AuthorizationTokenRepository {
    fun save(token: AuthorizationToken)
}
