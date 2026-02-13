package cz.grimir.wifimanager.captive.web.security

data class LdapLoginResult(
    val success: Boolean,
    val failureReason: LdapLoginFailureReason? = null,
)
