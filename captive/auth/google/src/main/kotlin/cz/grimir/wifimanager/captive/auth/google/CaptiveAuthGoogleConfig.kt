package cz.grimir.wifimanager.captive.auth.google

import cz.grimir.wifimanager.captive.application.port.UserAuthProviderPort
import cz.grimir.wifimanager.shared.application.identity.google.GoogleDirectoryApiClient
import cz.grimir.wifimanager.shared.application.identity.google.GoogleDirectoryApiProperties
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource

private val logger = KotlinLogging.logger {}

@Configuration
@Profile("google")
@EnableConfigurationProperties(GoogleLdapProperties::class)
@ConditionalOnProperty(
    prefix = "wifimanager.captive.auth.google",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class CaptiveAuthGoogleConfig {
    @Bean
    fun googleLdapAuthProvider(
        userDirectoryClient: UserDirectoryClient,
        properties: GoogleLdapProperties,
        directoryProperties: GoogleDirectoryApiProperties,
        directoryApiClient: GoogleDirectoryApiClient,
    ): UserAuthProviderPort {
        if (!properties.isConfigured(directoryProperties)) {
            logger.warn { "Google LDAP login disabled: missing search configuration or service-account-json-path." }
            return UserAuthProviderPort { null }
        }

        val allowedDevicesByGroup = properties.allowedDevicesByGroup()
        val contextSource = googleLdapContextSource(properties)
        val ldapTemplate = LdapTemplate(contextSource)
        val secureLdapClient = GoogleSecureLdapClient(ldapTemplate, properties)

        return GoogleLdapAuthProvider(
            secureLdapClient = secureLdapClient,
            directoryApiClient = directoryApiClient,
            userDirectoryClient = userDirectoryClient,
            allowedDevicesByGroup = allowedDevicesByGroup,
        )
    }

    private fun googleLdapContextSource(properties: GoogleLdapProperties): LdapContextSource {
        val source = LdapContextSource()
        source.setUrl(properties.ldapUrl)
        source.setBaseEnvironmentProperties(timeoutProperties(properties))
        source.afterPropertiesSet()
        return source
    }

    private fun timeoutProperties(properties: GoogleLdapProperties): Map<String, Any> =
        mapOf(
            "com.sun.jndi.ldap.connect.timeout" to properties.connectTimeout.toMillis().toString(),
            "com.sun.jndi.ldap.read.timeout" to properties.readTimeout.toMillis().toString(),
        )

    private fun GoogleLdapProperties.isConfigured(directoryProperties: GoogleDirectoryApiProperties): Boolean =
        userSearchBase.isNotBlank() && directoryProperties.isConfigured()
}
