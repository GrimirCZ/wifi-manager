package cz.grimir.wifimanager.captive.auth.keycloak

import cz.grimir.wifimanager.shared.application.KeyValueMapParser
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wifimanager.captive.auth.keycloak")
data class KeycloakPasswordGrantProperties(
    val issuerUri: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val allowedDevicesByRole: String = "",
) {
    fun isConfigured(): Boolean = issuerUri.isNotBlank() && clientId.isNotBlank() && clientSecret.isNotBlank()

    fun allowedDevicesByRole(): Map<String, Int> = KeyValueMapParser().parse(allowedDevicesByRole) { value -> value.toIntOrNull() }
}
