package cz.grimir.wifimanager.captive.auth.keycloak

import cz.grimir.wifimanager.captive.application.user.UserAuthProvider
import cz.grimir.wifimanager.shared.core.UserDirectoryClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.client.RestClient

private val logger = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(KeycloakPasswordGrantProperties::class)
@ConditionalOnProperty(
    prefix = "wifimanager.captive.auth.keycloak",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class CaptiveAuthKeycloakConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun keycloakPasswordGrantAuthProvider(
        properties: KeycloakPasswordGrantProperties,
        userDirectoryClient: UserDirectoryClient,
    ): UserAuthProvider {
        if (!properties.isConfigured()) {
            logger.warn { "Keycloak password grant login disabled: missing issuer-uri, client-id, or client-secret." }
            return UserAuthProvider { null }
        }

        val restClient = RestClient.builder().baseUrl(properties.issuerUri).build()
        return KeycloakPasswordGrantAuthProvider(restClient, properties, userDirectoryClient)
    }
}
