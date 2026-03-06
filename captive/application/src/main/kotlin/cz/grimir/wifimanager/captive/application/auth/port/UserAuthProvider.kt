package cz.grimir.wifimanager.captive.application.auth.port

import cz.grimir.wifimanager.captive.application.auth.model.UserAuthenticationResult
import cz.grimir.wifimanager.captive.application.auth.model.UserCredentials

fun interface UserAuthProvider {
    fun authenticate(credentials: UserCredentials): UserAuthenticationResult?
}
