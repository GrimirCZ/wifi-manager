package cz.grimir.wifimanager.shared.security.mvc

import org.slf4j.MDC

object RequestMdc {
    private const val USER_EMAIL_KEY = "userEmail"
    private const val USER_ID_KEY = "userId"
    private const val PRINCIPAL_KEY = "principal"
    private const val CLIENT_MAC_KEY = "clientMac"

    fun putUser(sessionUserIdentity: SessionUserIdentity) {
        MDC.put(USER_EMAIL_KEY, sessionUserIdentity.email)
        MDC.put(USER_ID_KEY, sessionUserIdentity.userId.toString())
        MDC.put(PRINCIPAL_KEY, "email=${sessionUserIdentity.email} userId=${sessionUserIdentity.userId}")
    }

    fun putDevice(macAddress: String) {
        if (macAddress.isBlank()) return
        MDC.put(CLIENT_MAC_KEY, macAddress)
    }

    fun clear() {
        MDC.remove(USER_EMAIL_KEY)
        MDC.remove(USER_ID_KEY)
        MDC.remove(PRINCIPAL_KEY)
        MDC.remove(CLIENT_MAC_KEY)
    }
}
