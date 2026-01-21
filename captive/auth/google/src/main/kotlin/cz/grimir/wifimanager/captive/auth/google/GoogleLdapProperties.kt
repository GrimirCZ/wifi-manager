package cz.grimir.wifimanager.captive.auth.google

import cz.grimir.wifimanager.shared.application.KeyValueMapParser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private val logger = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "wifimanager.captive.auth.google")
data class GoogleLdapProperties(
    val ldapUrl: String,
    val userSearchBase: String,
    val userSearchFilter: String = "mail={0}",
    val directoryCustomer: String,
    val serviceAccountJsonPath: String,
    val impersonatedUser: String? = null,
    val directoryApiBaseUrl: String = "https://admin.googleapis.com/admin/directory/v1",
    private val allowedDevicesByGroup: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(5),
    val readTimeout: Duration = Duration.ofSeconds(5),
) {
    fun allowedDevicesByGroup(): Map<String, Int> =
        KeyValueMapParser().parse(allowedDevicesByGroup) { it.toIntOrNull() }.also {
            if (it.isEmpty()) {
                logger.warn { "Allowed device limits list is empty; no devices will be allowed." }
            }
        }
}
