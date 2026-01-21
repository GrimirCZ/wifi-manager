package cz.grimir.wifimanager.web.captive.security

data class LdapLoginResult(
    val success: Boolean,
    val failureReason: LdapLoginFailureReason? = null,
)
