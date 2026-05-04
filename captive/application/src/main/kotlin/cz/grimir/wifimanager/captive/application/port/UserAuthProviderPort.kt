package cz.grimir.wifimanager.captive.application.port

import cz.grimir.wifimanager.captive.application.command.model.UserAuthenticationResult
import cz.grimir.wifimanager.captive.application.command.model.UserCredentials

fun interface UserAuthProviderPort {
    fun authenticate(credentials: UserCredentials): UserAuthenticationResult?
}
