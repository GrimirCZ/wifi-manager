package cz.grimir.wifimanager.captive.web.security

import cz.grimir.wifimanager.shared.core.UserId

data class LdapLoginResult(
    val success: Boolean,
    val failureReason: LdapLoginFailureReason? = null,
    val userId: UserId? = null,
)
