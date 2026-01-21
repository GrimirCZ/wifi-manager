package cz.grimir.wifimanager.captive.application.user

fun interface UserAuthProvider {
    fun authenticate(credentials: UserCredentials): UserAuthenticationResult?
}
