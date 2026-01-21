package cz.grimir.wifimanager.captive.auth.google

import cz.grimir.wifimanager.captive.application.user.UserCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.support.LdapEncoder
import java.text.MessageFormat

class GoogleSecureLdapClient(
    private val ldapTemplate: LdapTemplate,
    private val properties: GoogleLdapProperties,
) {
    private val logger = KotlinLogging.logger {}

    fun authenticate(credentials: UserCredentials): Boolean =
        try {
            ldapTemplate.authenticate(
                properties.userSearchBase,
                getSearchFilter(credentials.username),
                credentials.password,
            )
        } catch (ex: Exception) {
            logger.info(ex) { "Secure LDAP bind failed for ${credentials.username}" }
            false
        }

    private fun getSearchFilter(username: String): String =
        MessageFormat.format(
            properties.userSearchFilter,
            LdapEncoder.filterEncode(username),
        )
}
